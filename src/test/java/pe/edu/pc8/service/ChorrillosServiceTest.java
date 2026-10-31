package pe.edu.pc8.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChorrillosServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void poligonoConHueco() throws Exception {
        var g = mapper.readTree("""
            [
                [[0,0],[10,0],[10,10],[0,10],[0,0]],
                [[3,3],[7,3],[7,7],[3,7],[3,3]]
            ]
            """);

        assertTrue(ChorrillosService.poligono(g, 2, 2));
        assertFalse(ChorrillosService.poligono(g, 5, 5));
        assertFalse(ChorrillosService.poligono(g, 12, 12));
    }
}
