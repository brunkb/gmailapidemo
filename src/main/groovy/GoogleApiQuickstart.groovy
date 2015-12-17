import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.webmasters.Webmasters
import com.google.api.services.webmasters.WebmastersScopes
import com.google.api.services.webmasters.model.SitesListResponse
import com.google.api.services.webmasters.model.WmxSite

/**
 * This class is the advanced proof of concept.  It is also the runner I used to generate the StoredCredential files that
 * we place on the server.
 */
class GoogleApiQuickstart {

    static final String APPLICATION_NAME = "GMailDemo"

    static final String credentialsDirectory = 'src/main/resources/stored_credentials'

    static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance()

    static final List SCOPES = [GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY,
                                WebmastersScopes.WEBMASTERS_READONLY, WebmastersScopes.WEBMASTERS]

    static Credential authorize(Integer accountNumber) {

        String acctNumberString = accountNumber < 10 ? "0${accountNumber}" : accountNumber.toString()
        FileDataStoreFactory dataStoreFactory

        File dataStoreDirectory = new File("${credentialsDirectory}/gwt_${acctNumberString}")

        def userId = "FindLaw.GWT.${accountNumber}@gmail.com"

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        dataStoreFactory = new FileDataStoreFactory(dataStoreDirectory)

        // Load client secrets.
        String inp = new File("${credentialsDirectory}/gwt_${acctNumberString}/client_id.json").getText()
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new StringReader(inp))

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(dataStoreFactory)
                        .setAccessType("offline")
                        .build()

        // If there is no StoredCredential file already, Chrome will open a tab for you to manually
        // authorize the interaction.  The trick is that if your browser is already logged in to a particular
        // account, you must first logout of that account and then authorize the scope on the correct account by
        // first logging in to that account.
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize(userId)

        println("Credentials saved to: ${credentialsDirectory}/gwt_${acctNumberString}" )
        println "expiration: ${(long) credential.expiresInSeconds}"
        println "access token: ${credential.getAccessToken()}"
        println "refresh token: ${credential.getRefreshToken()}"

        return credential
    }

    static Gmail getGmailService(Credential credential) throws IOException {
        HttpTransport httpTransport =  GoogleNetHttpTransport.newTrustedTransport()
        new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    static Webmasters getWebmastersService(Credential credential) {
        // Create a new authorized API client
        HttpTransport httpTransport =  GoogleNetHttpTransport.newTrustedTransport()
        new Webmasters.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    static void main(String[] args) throws IOException {

        println "Starting up..."

            1.upto(53) { Integer accountNumber ->

            Credential credential = authorize(accountNumber)

            Gmail gmailService = getGmailService(credential)
            listMessagesMatchingQuery(gmailService)

            Webmasters webmasterService = getWebmastersService(credential)

            def verifiedSites = retrieveVerifiedSites(webmasterService)

            verifiedSites.eachWithIndex { String currentSite, int idx ->
                if(idx > 10 ) { return } // just print the first 10 and then bail out
                println currentSite
            }


        }
    }

    static List listMessagesMatchingQuery(Gmail gmailService) throws IOException {

        // NOTE:  The userId value has to be "me" or this won't work right with multiple accounts
        ListMessagesResponse response = gmailService.users().messages().list("me").execute()

        List messages = []
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages())
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken()
                response = gmailService.users().messages().list("me")
                        .setPageToken(pageToken).execute()
            } else {
                break
            }
        }

        println("messages size: ${messages.size()}")
        messages.eachWithIndex { Message message, int idx ->

            if (idx > 10) {  // print 10 and bail
                return
            }

            def contents = gmailService.users().messages().get("me", message.id).execute()

            def payload = contents.getPayload()
            def headers = payload['headers']

            def subject = headers.find { it.name == 'Subject' }
            def from = headers.find { it.name == 'From' }

            if (from.value in ['account-verification-noreply@google.com',
                               'sc-noreply@google.com',
                               'wmt-noreply@google.com',
                               'wmx-noreply@google.com'
            ]) {
                println "from: ${from.value} subject: ${subject?.value} "
            }
        }

        return messages
    }

    static List retrieveVerifiedSites(Webmasters webmasterService) {

        Webmasters.Sites.List request = webmasterService.sites().list()

        SitesListResponse siteList = request.execute()

        def verifiedSites = []
        siteList.getSiteEntry().each { WmxSite currentSite ->
           String permissionLevel = currentSite.permissionLevel
           if (permissionLevel.equals("siteOwner")) {
                verifiedSites << currentSite.siteUrl
           }
        }
        println "verfied sites size: ${verifiedSites.size()}"
        verifiedSites
    }
}