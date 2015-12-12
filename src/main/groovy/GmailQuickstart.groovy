import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import groovy.json.JsonSlurper

import java.security.PrivateKey

class GmailQuickstart {

    //static final String CLIENT_SECRET_FILE = 'client_secret_926004129130-jacpsrrcju45m445bsjsuuc5lvi8vucp.apps.googleusercontent.com.json'

    /** Application name. */
    static final String APPLICATION_NAME = "GMailDemo"

    /** Directory to store user credentials for this application. */
//    static final java.io.File DATA_STORE_DIR = new java.io.File(
//            System.getProperty("user.home"), ".credentials/gmail-java-quickstart")

    static FileDataStoreFactory DATA_STORE_FACTORY

    static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance()


    static final List SCOPES = [GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY]

    static Credential authorize() throws IOException {
        HttpTransport HTTP_TRANSPORT
        FileDataStoreFactory DATA_STORE_FACTORY

        java.io.File DATA_STORE_DIR = new java.io.File(
                System.getProperty("user.home"), ".credentials/gmail-java-quickstart")
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR)
        } catch (Throwable t) {
            t.printStackTrace()
            System.exit(1)
        }

        // Load client secrets.
        InputStream inp = new FileInputStream("src/main/resources/client_secret_599287201074-ijri5tfq3m6l8rtpihj07eahuedqksbv.apps.googleusercontent.com.json")
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
        return credential
    }

    static Gmail getGmailService() throws IOException {
        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

        Credential credential = authorize()
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
//
//
//        def creds = new FileInputStream("src/main/resources/GWTData-5ea530dd9107.json")
//
//        Credential credential = GoogleCredential.fromStream(creds)
//
//        return Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build()
    }

    static void main(String[] args) throws IOException {

        println "Starting up..."

        // Build a new authorized API client service.
        Gmail service = getGmailService()

        listMessagesMatchingQuery(service, "me")

        // Print the labels in the user's account.
//        String user = "me"
//        ListLabelsResponse listResponse =
//                service.users().labels().list(user).execute()
//        List<Label> labels = listResponse.getLabels()
//        if (labels.size() == 0) {
//            System.out.println("No labels found.")
//        } else {
//            System.out.println("Labels:")
//            labels.each {
//                println("- ${it.name}")
//            }
//        }
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

        messages.each { Message message ->

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
                println("from: ${from.value} subject: ${subject?.value} ")
            }


        }

        return messages
    }


}