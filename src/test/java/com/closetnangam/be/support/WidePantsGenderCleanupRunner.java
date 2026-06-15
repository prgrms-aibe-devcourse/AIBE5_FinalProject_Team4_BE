package com.closetnangam.be.support;

import com.closetnangam.be.global.external.naver.support.WidePantsGenderCorrector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 와이드 팬츠 MALE 오분류를 FEMALE로 되돌린다.
 * 로컬/공유 DB 정리용: {@code RUN_DB_MAINTENANCE=true} 와 {@code DB_USERNAME}/{@code DB_PASSWORD} 필요.
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_MAINTENANCE", matches = "true")
class WidePantsGenderCleanupRunner {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3307/closetnangamdb?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";

    @Test
    void correctMisclassifiedWidePantsGenderToFemale() throws Exception {
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        if (username == null || password == null) {
            username = "root";
            password = "local1234";
        }

        List<Long> targetIds = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, username, password)) {
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT clothes_id, brand_name, name, category, gender
                    FROM clothes
                    WHERE clothes_info_source = 'EXTERNAL_SHOPPING'
                      AND gender IN ('MALE', 'UNISEX')
                      AND category = 'BOTTOM'
                    """)) {
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        if (WidePantsGenderCorrector.shouldCorrectToFemale(
                                rs.getString("gender"),
                                rs.getString("category"),
                                rs.getString("brand_name"),
                                rs.getString("name")
                        )) {
                            targetIds.add(rs.getLong("clothes_id"));
                        }
                    }
                }
            }

            if (targetIds.isEmpty()) {
                System.out.println("No misclassified wide pants found.");
                return;
            }

            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE clothes
                    SET gender = 'FEMALE'
                    WHERE clothes_id = ?
                    """)) {
                for (Long clothesId : targetIds) {
                    update.setLong(1, clothesId);
                    update.addBatch();
                }
                update.executeBatch();
            }

            System.out.printf("Updated %d wide pants rows to FEMALE gender.%n", targetIds.size());
        }
    }
}
