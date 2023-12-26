package org.example;

import com.google.auth.oauth2.GoogleCredentials;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Main {

    private static final String PROJECT_ID = "<YOUR-PROJECT-ID>";
    private static final String BASE_URL = "https://fcm.googleapis.com";
    private static final String FCM_SEND_ENDPOINT = "/v1/projects/" + PROJECT_ID + "/messages:send";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = { MESSAGING_SCOPE };
    private static final String TITLE = "FCM Notification";
    private static final String BODY = "Notification from FCM";
    public static void main(String[] args) {
        try (HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build()) {

            // アクセストークンリストの取得
            List<String> tokens = generateTokens(10);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (String token : tokens) {
                HttpRequest request = buildHttpRequest(token);
                CompletableFuture<Void> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(System.out::println);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** ランダムに疑似的なアクセストークンを引数の数だけ生成するメソッドです
     * @param num 生成するアクセストークンの数
     * @return アクセストークンのリスト
     */
    private static List<String> generateTokens (int num) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            tokens.add(String.valueOf(i));
        }
        return tokens;
    }

    /** HttpRequestの作成
     * @param token デバイストークン
     */
    @NotNull
    private static HttpRequest buildHttpRequest(@NotNull String token) throws IOException {
        return HttpRequest.newBuilder()
                .POST(buildHttpRequestBodyMulti(token))
                .uri(URI.create(BASE_URL + FCM_SEND_ENDPOINT))
                .setHeader("Authorization", STR."Bearer \{getAccessToken()}")
                .setHeader("Content-Type", "application/json; UTF-8")
                .build();
    }

    /** サービスアカウントの秘密鍵を使ってアクセストークンを取得する
     * @return アクセストークン
     * @throws IOException ファイルが存在しない場合
     */
    private static String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream("service-account.json"))
                .createScoped(Arrays.asList(SCOPES));
        googleCredentials.refreshAccessToken();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    /** 複数のデバイストークンに同じ内容のメッセージを送信する場合
     * @param token デバイストークン
     * @return リクエストボディ
     */
    private static HttpRequest.BodyPublisher buildHttpRequestBodyMulti(String token) {
        return HttpRequest.BodyPublishers.ofString(STR."""
                {
                    "message":{
                        "token" : "\{token}",
                        "notification" : {
                            "title" : "\{TITLE}",
                            "body" : "\{BODY}"
                        },
                        "data" : {
                            "score" : "850",
                            "time" : "2:45"
                        }
                    }
                }
                """);
    }
}