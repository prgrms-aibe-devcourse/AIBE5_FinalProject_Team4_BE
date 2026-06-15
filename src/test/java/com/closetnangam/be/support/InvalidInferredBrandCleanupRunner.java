package com.closetnangam.be.support;

import com.closetnangam.be.global.external.naver.support.BrandNameSanitizer;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * EXTERNAL_SHOPPING의 잘못 추론된 brand_name을 UNKNOWN으로 되돌린다.
 * 로컬/공유 DB 정리용: {@code DB_USERNAME}/{@code DB_PASSWORD} 환경 변수 필요.
 */
class InvalidInferredBrandCleanupRunner {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3307/closetnangamdb?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";

    @Test
    void revertInvalidInferredBrandsToUnknown() throws Exception {
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        if (username == null || password == null) {
            username = "root";
            password = "local1234";
        }

        List<Long> targetIds = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, username, password)) {
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT clothes_id, brand_name
                    FROM clothes
                    WHERE clothes_info_source = 'EXTERNAL_SHOPPING'
                      AND brand_name <> 'UNKNOWN'
                    """)) {
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        if (BrandNameSanitizer.isLikelyProductDescriptor(rs.getString("brand_name"))) {
                            targetIds.add(rs.getLong("clothes_id"));
                        }
                    }
                }
            }

            if (targetIds.isEmpty()) {
                System.out.println("No invalid inferred brands found.");
                return;
            }

            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE clothes
                    SET brand_name = 'UNKNOWN'
                    WHERE clothes_id = ?
                    """)) {
                for (Long clothesId : targetIds) {
                    update.setLong(1, clothesId);
                    update.addBatch();
                }
                update.executeBatch();
            }

            System.out.printf("Reverted %d EXTERNAL_SHOPPING rows to UNKNOWN brand_name.%n", targetIds.size());
        }
    }
}
