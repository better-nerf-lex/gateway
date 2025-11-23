package com.bntech.lexai.gateway.data.object;

public class Prompts {

    public static final String PL_USER_MESSAGE = "Użytkownik chce stworzyćten dokument: ";
    public static final String EN_USER_MESSAGE = "User wants to create this document: ";

    public static final String PL_GENERATE_QUESTIONS = """
                Twoim zadaniem jest przygotowanie danych wejściowych do wygenerowania dokumentu na podstawie wzoru graficznego (szablonu), który otrzymasz w załączniku jako obraz.
                Wygeneruj odpowiedź w formacie JSON, zgodnie z poniższym schematem:
                {
                    "questions": ["pytanie1", "pytanie2", ...],
                    "base": ["podstawaPrawna1", "podstawaPrawna2"],
                    "name": "sugerowanaNazwaWniosku",
                    "requirements": ["wymaganieDotycząceDokumentu1", "wymaganie2"]
                }
            
                Krok po kroku:
                1. Przeanalizuj obraz (szablon) i określ, jakie dane są wymagane do jego wypełnienia.
                2. Podaj listę wymaganych informacji w polu `questions`.
                3. Uwzględnij również pytania o dane takie jak sygnatura sprawy lub nazwisko panieńskie matki **tylko jeśli są one niezbędne** do wniosku.
                4. W polu `base` wskaż podstawy prawne z art. 235 Kodeksu postępowania cywilnego: https://sip.lex.pl/akty-prawne/dzu-dziennik-ustaw/kodeks-postepowania-cywilnego-16786199/art-235
                5. W polu `name` zaproponuj odpowiednią nazwę dla dokumentu.
                6. W `requirements` wpisz, jakie załączniki lub dokumenty muszą zostać dostarczone.
            
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
            4. At the end, include a section titled "Podstawa prawna", listing all elements from "base" as legal bases.
            
            Additional notes:
            - Use only formal legal language, no casual or informal tone.
            - Do not include any explanation, metadata, or comments.
            - Output only the final document content, formatted for direct PDF rendering.
            
            JSON input:
            
             %s
            """;


    public static final String EN_GENERATE_QUESTIONS = """
                Your task is to prepare structured input data to generate a document based on a graphic template (form image) that will be provided as an attachment.
                All in Polish.
                Respond strictly in JSON format, using the following schema:
                {
                    "questions": ["question1", "question2", ...],
                    "base": ["legalBasis1", "legalBasis2"],
                    "name": "suggestedApplicationName",
                    "requirements": ["documentRequirement1", "documentRequirement2"]
                }
            
                Step-by-step instructions:
                1. Analyze the image (template) to determine which fields and data points are required.
                2. Extract all data fields as user-facing questions and list them under `questions`.
                3. Include questions for personal data such as case reference number or mother’s maiden name **only if** they are required for this specific type of form.
                4. Under `base`, list the legal bases, specifically using Article 235 of the Polish Code of Civil Procedure: https://sip.lex.pl/akty-prawne/dzu-dziennik-ustaw/kodeks-postepowania-cywilnego-16786199/art-235
                5. In the `name` field, provide a suggested name for the application or request.
                6. In `requirements`, list the necessary supporting documents or attachments.
            
                Return only the JSON data. Do not include any explanations, comments, or additional content.
                Remember that data like applicant, date it was created and legal entity to which the application is directed must be provided.
                There is no need to signature space.
            """;
}
