package com.jakdang.labs.api.jaegyeom.auth.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class VerificationCodeStore {
    
    private final Map<String, String> codeStore = new ConcurrentHashMap<>();

    public void saveCode(String email, String code) {
        codeStore.put(email, code);
    }
    
    public boolean verifyCode(String email, String code) {
        return code.equals(codeStore.get(email));
    }

    public void removeCode(String email) {
        codeStore.remove(email);
    }
}
