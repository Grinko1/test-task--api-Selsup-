package app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        long period = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(period);
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void createProduct(Document document, String signature) {
        try {
            semaphore.acquire();
            String jsonInputString = JsonMapper.toJson(document);
            int responseCode = DocumentEndpoints.createProduct(jsonInputString, signature);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Document created: " + document);
            } else {
                System.out.println("Response Code: " + responseCode+ ". " + "Failed to create document: " + document);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);

//      Mock document
        Document document = new Document(new Description("description"), "docId", "docStatus", "docType", true, "ownerInn", "participantInn", "producerInn", new Date(2023, Calendar.MAY, 25), "productionType",
                new Product[]{new Product("certificate", new Date(2023, Calendar.MAY, 12), "certificateDocumentNumber", "ownerInn", "producerInn", new Date(2023, 4, 12),
                        "tnvedCode", "uitCode", "uituCode"
                )}, new Date(2023, 4, 12), "regNumber");


            new Thread(() -> {
                crptApi.createProduct(document, "signature");
            }).start();
        }
}

class DocumentEndpoints {
    public static int createProduct(String jsonInputString, String signature) {
        try {
            URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Signature", signature);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            return connection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}

class JsonMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();
    static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}


@Data
@AllArgsConstructor
class Document {
    @JsonProperty("description")
    private Description description;
    @JsonProperty("doc_id")
    private String docId;
    @JsonProperty("doc_status")
    private String docStatus;
    @JsonProperty("doc_type")
    private String docType;
    @JsonProperty("importRequest")
    private boolean importRequest;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("participant_inn")
    private String participantInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    private Date productionDate;
    @JsonProperty("production_type")
    private String productionType;
    @JsonProperty("products")
    private Product[] products;
    @JsonProperty("reg_date")
    private Date regDate;
    @JsonProperty("reg_number")
    private String regNumber;

}


@Data
@AllArgsConstructor
class Description {
    @JsonProperty("participantInn")
    private String participantInn;

}

@Data
@AllArgsConstructor
class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;
    @JsonProperty("certificate_document_date")
    private Date certificateDocumentDate;
    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    private Date productionDate;
    @JsonProperty("tnved_code")
    private String tnvedCode;
    @JsonProperty("uit_code")
    private String uitCode;
    @JsonProperty("uitu_code")
    private String uituCode;


}