//package com.jakdang.labs.config;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.messaging.FirebaseMessaging;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
//import org.springframework.web.servlet.tags.form.InputTag;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//@Configuration
//@Slf4j
//public class FirebaseInitializer {
//
//    @Bean
//    public FirebaseApp firebaseApp() throws IOException {
//        log.info("Initializing Firebase.");
//        InputStream serviceAccount =
//                getClass().getClassLoader().getResourceAsStream("firebase-adminsdk.json");
//
//        FirebaseOptions options = new FirebaseOptions.Builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build();
//
//        return FirebaseApp.initializeApp(options);
//    }
//
//    @Bean
//    public FirebaseMessaging getFirebaseMessaging() throws IOException {
//        return FirebaseMessaging.getInstance(firebaseApp());
//    }
//
//    @Bean
//    public FirebaseAuth getFirebaseAuth() throws IOException {
//        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp());
//        return firebaseAuth;
//    }
//}