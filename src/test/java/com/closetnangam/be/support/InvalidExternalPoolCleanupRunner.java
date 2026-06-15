package com.closetnangam.be.support;

import com.closetnangam.be.domain.recommendation.support.ComplementaryRecommendationProductFilter;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * EXTERNAL_SHOPPING 공용 풀에서 비의류·키워드 stuffing·잘못된 브랜드 상품을 삭제한다.
 * 옷장/코디에서 참조 중인 CLOTHES는 건너뛴다.
 */
class InvalidExternalPoolCleanupRunner {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3307/closetnangamdb?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";

    @Test
    void removeInvalidExternalPoolProducts() throws Exception {
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        if (username == null || password == null) {
            username = "root";
            password = "local1234";
        }

        List<Long> targetIds = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, username, password)) {
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT clothes_id, brand_name, name
                    FROM clothes
                    WHERE clothes_info_source = 'EXTERNAL_SHOPPING'
                    """)) {
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        long clothesId = rs.getLong("clothes_id");
                        if (isReferenced(connection, clothesId)) {
                            continue;
                        }
                        if (ComplementaryRecommendationProductFilter.shouldExcludeFromExternalPool(
                                rs.getString("name"),
                                rs.getString("brand_name")
                        )) {
                            targetIds.add(clothesId);
                        }
                    }
                }
            }

            if (targetIds.isEmpty()) {
                System.out.println("No invalid external pool products found.");
                return;
            }

            try (PreparedStatement deleteColors = connection.prepareStatement(
                    "DELETE FROM clothing_colors WHERE clothes_id = ?");
                 PreparedStatement deleteStyles = connection.prepareStatement(
                         "DELETE FROM clothing_styles WHERE clothes_id = ?");
                 PreparedStatement deleteClothes = connection.prepareStatement(
                         "DELETE FROM clothes WHERE clothes_id = ?")) {
                for (Long clothesId : targetIds) {
                    deleteColors.setLong(1, clothesId);
                    deleteColors.addBatch();
                    deleteStyles.setLong(1, clothesId);
                    deleteStyles.addBatch();
                    deleteClothes.setLong(1, clothesId);
                    deleteClothes.addBatch();
                }
                deleteColors.executeBatch();
                deleteStyles.executeBatch();
                deleteClothes.executeBatch();
            }

            System.out.printf("Removed %d invalid EXTERNAL_SHOPPING pool products.%n", targetIds.size());
        }
    }

    private static boolean isReferenced(Connection connection, long clothesId) throws Exception {
        try (PreparedStatement wardrobe = connection.prepareStatement(
                "SELECT 1 FROM wardrobe_clothes WHERE clothes_id = ? LIMIT 1")) {
            wardrobe.setLong(1, clothesId);
            try (ResultSet rs = wardrobe.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        try (PreparedStatement outfit = connection.prepareStatement(
                "SELECT 1 FROM outfit_items WHERE clothes_id = ? LIMIT 1")) {
            outfit.setLong(1, clothesId);
            try (ResultSet rs = outfit.executeQuery()) {
                return rs.next();
            }
        }
    }
}
