import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message

/**
 * Created by uc192330 on 12/9/2015.
 */
class GMailQuickStartServiceAccount {

    /** Application name. */
    static final String APPLICATION_NAME = "GWTData"

    static Gmail getGmailService() throws IOException {

        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance()

        //def creds = new FileInputStream("src/main/resources/GWT23.json")

        def c = """
            {
            <secret info>
            }

        """

        GoogleCredential credential = GoogleCredential.fromStream(new ByteArrayInputStream(c.getBytes()),
                HTTP_TRANSPORT, JSON_FACTORY)
        credential.serviceAccountUser = 'FindLaw.GWT.23@gmail.com'
        credential.serviceAccountScopes = [GmailScopes.MAIL_GOOGLE_COM,
                                           GmailScopes.GMAIL_LABELS,
                                           GmailScopes.GMAIL_READONLY,
                                            GmailScopes.GMAIL_MODIFY]


        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    static void main(String[] args) throws IOException {

        println "Starting up..."

        // Build a new authorized API client service.
        Gmail service = getGmailService()

        listMessagesMatchingQuery(service, "me")
    }

    static List executeApiCall(Gmail service, String userId) {
        println "trying service call"
        service.users().messages().list(userId).execute()
    }

    static List listMessagesMatchingQuery(Gmail service, String userId) throws IOException {

        ListMessagesResponse response
        boolean shouldRetry = true
        int retryCount = 0

        while (retryCount < 5 && shouldRetry) {
            try {

                response = executeApiCall(service, userId)
                shouldRetry = false
            } catch (TokenResponseException  e) {
                println e
                retryCount += 1
                Thread.sleep(300)
            }
         }

        if(!response) {
            println "Bailing out!"
            System.exit(1)
        }

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
