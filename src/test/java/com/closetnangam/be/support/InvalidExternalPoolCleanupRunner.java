package com.closetnangam.be.support;

import com.closetnangam.be.domain.recommendation.support.ComplementaryRecommendationProductFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * EXTERNAL_SHOPPING 공용 풀에서 비의류·키워드 stuffing·잘못된 브랜드 상품을 삭제한다.
 * <p>
 * 참조 정책:
 * <ul>
 *   <li>{@code wardrobe_clothes}, {@code outfit_items} 참조가 있으면 삭제하지 않는다.</li>
 *   <li>{@code recommendation_feedbacks}만 참조하는 invalid 풀 상품은 피드백 행을 먼저 삭제한 뒤
 *       태그·clothes를 같은 트랜잭션에서 제거한다. (공유 풀에서 제거 대상이므로 고아 피드백도 정리)</li>
 * </ul>
 * {@code RUN_DB_MAINTENANCE=true} 일 때만 실행 (CI 기본 test 제외).
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_MAINTENANCE", matches = "true")
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
                        if (isReferencedByUserData(connection, clothesId)) {
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

            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteFeedbacks = connection.prepareStatement(
                        "DELETE FROM recommendation_feedbacks WHERE clothes_id = ?");
                     PreparedStatement deleteColors = connection.prepareStatement(
                             "DELETE FROM clothing_colors WHERE clothes_id = ?");
                     PreparedStatement deleteStyles = connection.prepareStatement(
                             "DELETE FROM clothing_styles WHERE clothes_id = ?");
                     PreparedStatement deleteClothes = connection.prepareStatement(
                             "DELETE FROM clothes WHERE clothes_id = ?")) {
                    for (Long clothesId : targetIds) {
                        deleteFeedbacks.setLong(1, clothesId);
                        deleteFeedbacks.addBatch();
                        deleteColors.setLong(1, clothesId);
                        deleteColors.addBatch();
                        deleteStyles.setLong(1, clothesId);
                        deleteStyles.addBatch();
                        deleteClothes.setLong(1, clothesId);
                        deleteClothes.addBatch();
                    }
                    deleteFeedbacks.executeBatch();
                    deleteColors.executeBatch();
                    deleteStyles.executeBatch();
                    deleteClothes.executeBatch();
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            System.out.printf("Removed %d invalid EXTERNAL_SHOPPING pool products.%n", targetIds.size());
        }
    }

    private static boolean isReferencedByUserData(Connection connection, long clothesId) throws Exception {
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
