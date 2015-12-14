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

class GoogleApiQuickstart {

    /** Application name. */
    static final String APPLICATION_NAME = "GMailDemo"

    static HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

    static final String credentialsDirectory = 'src/main/resources/stored_credentials'

    static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance()

    static final List SCOPES = [GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY,
                                WebmastersScopes.WEBMASTERS_READONLY]

    static Credential authorize() throws IOException {
        HttpTransport HTTP_TRANSPORT
        FileDataStoreFactory DATA_STORE_FACTORY

        File DATA_STORE_DIR = new File("${credentialsDirectory}/gwt_24")
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR)
        } catch (Throwable t) {
            t.printStackTrace()
            System.exit(1)
        }

        // Load client secrets.
        InputStream inp = new FileInputStream("${credentialsDirectory}/gwt_24/client_id.json")
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inp))

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build()
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user")
        //println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath())

        // check access token for expiration
        // if access token is expired, use refresh token to get a new access token
        println "expiration: ${(long) credential.expiresInSeconds}"
        println "refresh token: ${credential.getRefreshToken()}"

        return credential
    }

    static Gmail getGmailService(Credential credential) throws IOException {
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()

    }

    static Webmasters getWebmastersService(Credential credential) {
        // Create a new authorized API client
        return new Webmasters.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }


    static void main(String[] args) throws IOException {

        println "Starting up..."

        // Build a new authorized API client service.
        Credential credential = authorize()
        Gmail gmailService = getGmailService(credential)

        //listMessagesMatchingQuery(gmailService, "me")

        Webmasters webmasterService = getWebmastersService(credential)

        def verifiedSites = retrieveVerifiedSites(webmasterService)

        verifiedSites.each { String currentSite ->
            println currentSite
        }

    }

    static List listMessagesMatchingQuery(Gmail service, String userId) throws IOException {
        ListMessagesResponse response = service.users().messages().list(userId).execute()

        List messages = []
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages())
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken()
                response = service.users().messages().list(userId)
                        .setPageToken(pageToken).execute()
            } else {
                break
            }
        }

        messages.eachWithIndex { Message message, int idx ->

            if (idx > 10) {
                return
            }

            def contents = service.users().messages().get(userId, message.id).execute()

            def labels = contents.getLabelIds()
            //println labels


            def payload = contents.getPayload()
            def headers = payload['headers']
            //println headers
            //println headers.find { it.name == 'Subject' }

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

        // Get all sites that are verified
        def verifiedSites = []

        SitesListResponse siteList = request.execute()

        siteList.getSiteEntry().each { WmxSite currentSite ->
            String permissionLevel = currentSite.permissionLevel
            if (permissionLevel.equals("siteOwner")) {
                verifiedSites.add(currentSite.siteUrl)
            }
        }
        verifiedSites
    }
}