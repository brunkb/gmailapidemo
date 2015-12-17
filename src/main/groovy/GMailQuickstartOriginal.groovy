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
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message

/**
 * This class is the example right off of Google's website with minimal modifications.  It was the first proof of concept.
 */
class GMailQuickstartOriginal {

    static
    final String CLIENT_SECRET_FILE = 'client_id.json'

    /** Application name. */
    static final String APPLICATION_NAME =
            "GMailDemo"

    /** Directory to store user credentials for this application. */
    static final File DATA_STORE_DIR = new File(
            System.getProperty("user.home"), ".credentials/gmail-java-quickstart")

    static FileDataStoreFactory DATA_STORE_FACTORY

    static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance()

    static HttpTransport HTTP_TRANSPORT

    static final List<String> SCOPES =
            Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY)

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR)
        } catch (Throwable t) {
            t.printStackTrace()
            System.exit(1)
        }
    }

    static Credential authorize() throws IOException {
        def clientSecretsInput = new FileInputStream(CLIENT_SECRET_FILE)

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(clientSecretsInput))

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build()
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user")
        println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath())
        return credential
    }

    static Gmail getGmailService() throws IOException {
        Credential credential = authorize()
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    static void main(String[] args) throws IOException {

        println "Starting up..."

        // Build a new authorized API client service.
        Gmail service = getGmailService()
        println "Listing messages"
        listMessagesMatchingQuery(service, "me")

        // Print the labels in the user's account.
        String user = "me"
        ListLabelsResponse listResponse =
                service.users().labels().list(user).execute()
        List<Label> labels = listResponse.getLabels()
        if (labels.size() == 0) {
            println("No labels found.")
        } else {
            println("Labels:")
            labels.each {
                println("- ${it.name}")
            }
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

        messages.each { Message message ->

            def contents = service.users().messages().get(userId, message.id).execute()

            def labels = contents.getLabelIds()
            println labels


            def payload = contents.getPayload()
            def headers = payload['headers']

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