package com.asistencias247.api;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMigrationTest {

    private static final String DB_URL = "jdbc:postgresql://ep-mute-forest-b524iw2m-pooler.c-7.us-east-2.aws.neon.tech/neondb?sslmode=require";
    private static final String DB_USER = "neondb_owner";
    private static final String DB_PASS = "npg_ULx8Sau6kGsB";

    @Test
    void verifyCatalogoMaterialesIsEliminatedAndMaterialesServicioPointsToMateriales() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // 1. catalogo_materiales NO debe existir
            ResultSet rsCheck = stmt.executeQuery("SELECT to_regclass('public.catalogo_materiales')");
            assertTrue(rsCheck.next());
            assertNull(rsCheck.getString(1), "catalogo_materiales debe haber sido eliminada");

            // 2. materiales_servicio debe tener FK hacia materiales
            ResultSet rsFk = stmt.executeQuery(
                "SELECT ccu.table_name AS foreign_table_name " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name " +
                "WHERE tc.table_name = 'materiales_servicio' AND tc.constraint_name = 'fk_materiales_servicio_material'"
            );
            assertTrue(rsFk.next(), "Constraint fk_materiales_servicio_material debe existir");
            assertEquals("materiales", rsFk.getString("foreign_table_name"), "La FK debe apuntar a la tabla materiales");
        }
    }
}
