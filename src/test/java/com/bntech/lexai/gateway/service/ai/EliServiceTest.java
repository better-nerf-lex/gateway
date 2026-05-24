package com.bntech.lexai.gateway.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EliServiceTest {

    private EliService eliService;

    @BeforeEach
    void setUp() {
        eliService = new EliService();
    }

    @Test
    void testBuildEliUrl_NewApi() throws Exception {
        java.lang.reflect.Method method = EliService.class.getDeclaredMethod("buildEliUrl", String.class);
        method.setAccessible(true);

        String uri = "eli/MP/1964/155/ogl";
        String url = (String) method.invoke(eliService, uri);
        assertThat(url).isEqualTo("https://eli.gov.pl/api/acts/MP/1964/155/text/I/M19640155.pdf");

        uri = "eli/DU/1964/93/ogl";
        url = (String) method.invoke(eliService, uri);
        assertThat(url).isEqualTo("https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf");

        uri = "eli/DU/2023/1/tj";
        url = (String) method.invoke(eliService, uri);
        assertThat(url).isEqualTo("https://eli.gov.pl/api/acts/DU/2023/1/text/U/D20230001.pdf");
    }

    @Test
    void testBuildEliUrl_Fallback() throws Exception {
        java.lang.reflect.Method method = EliService.class.getDeclaredMethod("buildEliUrl", String.class);
        method.setAccessible(true);

        String uri = "something/else";
        String url = (String) method.invoke(eliService, uri);
        assertThat(url).isEqualTo("https://eli.gov.pl/something/else/pol/html");
    }

    @Test
    void testBuildLegalDocumentsContext() {
        List<EliDocument> docs = List.of(
                new EliDocument("eli/DU/1964/93/ogl", "https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf"),
                new EliDocument("eli/MP/1964/155/ogl", "https://eli.gov.pl/api/acts/MP/1964/155/text/I/M19640155.pdf")
        );

        String context = eliService.buildLegalDocumentsContext(docs);

        assertThat(context).contains("--- PODSTAWY PRAWNE (Legal Basis) ---");
        assertThat(context).contains("eli/DU/1964/93/ogl");
        assertThat(context).contains("Link: https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf");
        assertThat(context).contains("eli/MP/1964/155/ogl");
        assertThat(context).contains("Link: https://eli.gov.pl/api/acts/MP/1964/155/text/I/M19640155.pdf");
    }
}
