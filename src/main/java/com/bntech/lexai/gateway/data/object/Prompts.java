package com.bntech.lexai.gateway.data.object;

public class Prompts {

    public static final String PL_USER_MESSAGE = "Użytkownik chce stworzyćten dokument: ";
    public static final String EN_USER_MESSAGE = "User wants to create this document: ";

    public static final String PL_GENERATE_QUESTIONS = """
                Twoim zadaniem jest przygotowanie danych wejściowych do wygenerowania dokumentu na podstawie wzoru graficznego (szablonu), który otrzymasz w załączniku jako obraz.
                Wygeneruj odpowiedź w formacie JSON, zgodnie z poniższym schematem:
                {
                    "questions": [
                        {
                            "id": "unikalny_id_1",
                            "text": "treść_pytania_1",
                            "type": "TEXT | PERSON | COMPANY | COURT | LAW_FIRM | ENTITY"
                        },
                        ...
                    ],
                    "base": ["eli/DU/rok/pozycja/ogl", "eli/DU/rok/pozycja/ogl"],
                    "name": "sugerowanaNazwaWniosku",
                    "requirements": ["wymaganieDotycząceDokumentu1", "wymaganie2"]
                }

                Krok po kroku:
                1. Przeanalizuj obraz (szablon) i określ, jakie dane są wymagane do jego wypełnienia.
                2. Podaj listę wymaganych informacji w polu `questions`.
                3. Dla każdego pytania przypisz unikalny `id` i określ jego `type`.
                4. Użyj `PERSON`, `COMPANY`, `COURT` lub `LAW_FIRM` jeśli pytanie dotyczy konkretnie takiego podmiotu. Użyj `ENTITY` dla ogólnych podmiotów prawnych. W pozostałych przypadkach użyj `TEXT`.
                5. Uwzględnij pytania o dane takie jak sygnatura sprawy lub nazwisko panieńskie matki **tylko jeśli są one niezbędne** do wniosku.
                6. W polu `base` wskaż podstawy prawne używając formatu ELI (European Legislation Identifier).
                   
                   WAŻNE: Formatuj podstawy prawne jako URI ELI według wzoru:
                   eli/{dziennik}/{rok}/{pozycja}/{ogl|tj|uj}
                   
                   Gdzie:
                   - dziennik: DU (Dziennik Ustaw), MP (Monitor Polski), itp.
                   - rok: rok publikacji (np. 1964, 2020)
                   - pozycja: numer pozycji w dzienniku
                   - ogl = tekst ogłoszony, tj = tekst jednolity, uj = tekst ujednolicony
                   
                   Przykłady:
                   - "eli/DU/1964/93/ogl" dla Kodeksu cywilnego
                   - "eli/DU/1964/43/ogl" dla Kodeksu postępowania cywilnego
                   - "eli/DU/1974/1/ogl" dla Kodeksu rodzinnego i opiekuńczego
                   - "eli/DU/1997/88/ogl" dla Kodeksu karnego
                   
                   Te URI pozwalają pobrać pełne teksty prawne z https://eli.gov.pl/
                   
                7. W polu `name` zaproponuj odpowiednią nazwę dla dokumentu.
                8. W `requirements` wpisz, jakie załączniki lub dokumenty muszą zostać dostarczone.

                Odpowiedź musi zawierać wyłącznie dane w formacie JSON. Nie dodawaj żadnego innego tekstu ani komentarzy.
                Pamiętaj, że dane takie jak szczegóły wnioskodawcy, data sporządzenia dokumentu oraz organ do którego kierowany jest wniosek są wymagane.
                Miejsca na podpis nie trzeba
            """;

    public static final String PL_GENERATE_DOCUMENT = """
            Wygeneruj HTMl ktory moge przerobić na PDF bez strachu zlozyc w sadzie.
            Wymiary strony A4, najwyżej kolejne strony.
            zawierajacy dane jak w questions,
            wyglada jak opsiane w requirements,
            tytul jak w name
            na koncu zawiera liste z base

                        %s
            """;

    public static final String EN_GENERATE_DOCUMENT = """
            Generate a formal legal document in Polish using HTML which I can do into pdf later, based on the provided JSON data. The document must be suitable for submission to court and ready for conversion to PDF.

            Formatting and content rules:
            1. Use the value of "name" as the exact document title.
            2. The main body of the document must contain the data from "questions", written in proper legal and formal style. Each {"question": "answer"} should be converted into a coherent paragraph.
            3. The document layout and appearance must follow the structure described in "requirements". Treat "requirements" as layout and formatting guidelines (e.g., margins, sections, titles).
            4. At the end, include a section titled "Podstawa prawna", listing all elements from "base" as legal bases with their ELI URIs formatted as clickable links to https://eli.gov.pl/{uri}.
            5. If legal documents are attached below (from eli.gov.pl), use them as authoritative reference for legal terminology, article citations, and proper legal phrasing. Cite specific articles when relevant.

            Additional notes:
            - Use only formal legal language, no casual or informal tone.
            - Do not include any explanation, metadata, or comments.
            - Output only the final document content, formatted for direct PDF rendering.
            - When referencing legal bases, include the full legal name of the act and specific article numbers.

            JSON input:

             %s
            """;

    public static final String EN_GENERATE_QUESTIONS = """
                Your task is to prepare structured input data to generate a document based on a graphic template (form image) that will be provided as an attachment.
                All in Polish.
                Respond strictly in JSON format, using the following schema:
                {
                    "questions": [
                        {
                            "id": "unique_id_1",
                            "text": "question_text_1",
                            "type": "TEXT | PERSON | COMPANY | COURT | LAW_FIRM | ENTITY"
                        },
                        ...
                    ],
                    "base": ["eli/DU/rok/pozycja/ogl", "eli/DU/rok/pozycja/ogl"],
                    "name": "suggestedApplicationName",
                    "requirements": ["documentRequirement1", "documentRequirement2"]
                }

                Step-by-step instructions:
                1. Analyze the image (template) to determine which fields and data points are required.
                2. Extract all data fields as user-facing questions and list them under `questions`.
                3. For each question, assign a unique `id` and determine its `type`.
                4. Use `PERSON`, `COMPANY`, `COURT`, or `LAW_FIRM` if the question specifically asks for details of such a legal entity. Use `ENTITY` if it's generic but clearly about a legal entity. Otherwise use `TEXT`.
                5. Include questions for personal data such as case reference number or mother’s maiden name **only if** they are required for this specific type of form.
                6. Under `base`, list the legal bases using ELI (European Legislation Identifier) URI format.
                   
                   IMPORTANT: Format legal bases as ELI URIs following this pattern:
                   eli/{dziennik}/{rok}/{pozycja}/{ogl|tj|uj}
                   
                   Where:
                   - dziennik: DU (Dziennik Ustaw), MP (Monitor Polski), etc.
                   - rok: year of publication (e.g., 1964, 2020)
                   - pozycja: position number in the journal
                   - ogl = original text, tj = consolidated text, uj = unified text
                   
                   Examples:
                   - "eli/DU/1964/93/ogl" for Kodeks cywilny (Civil Code)
                   - "eli/DU/1964/43/ogl" for Kodeks postępowania cywilnego
                   - "eli/DU/1974/1/ogl" for Kodeks rodzinny i opiekuńczy
                   - "eli/DU/1997/88/ogl" for Kodeks karny
                   
                   These URIs can be used to fetch full legal texts from https://eli.gov.pl/
                   
                7. In the `name` field, provide a suggested name for the application or request.
                8. In `requirements`, list the necessary supporting documents or attachments.

                Return only the JSON data. Do not include any explanations, comments, or additional content.
                Remember that data like applicant, date it was created and legal entity to which the application is directed must be provided.
                There is no need to signature space.
            """;
}
