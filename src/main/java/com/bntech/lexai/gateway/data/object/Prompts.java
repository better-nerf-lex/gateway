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

                   Zweryfikowane przykłady (używaj TYLKO tych URI):
                   - "eli/DU/1964/93/ogl" dla Kodeksu cywilnego
                   - "eli/DU/1964/296/ogl" dla Kodeksu postępowania cywilnego
                   - "eli/DU/1964/59/ogl" dla Kodeksu rodzinnego i opiekuńczego
                   - "eli/DU/1997/553/ogl" dla Kodeksu karnego
                   - "eli/DU/1997/555/ogl" dla Kodeksu postępowania karnego
                   - "eli/DU/1960/168/ogl" dla Kodeksu postępowania administracyjnego
                   - "eli/DU/1989/163/ogl" dla Prawa geodezyjnego i kartograficznego
                   - "eli/DU/1997/741/ogl" dla Ustawy o gospodarce nieruchomościami
                   - "eli/DU/1974/141/ogl" dla Kodeksu pracy
                   - "eli/DU/1994/414/ogl" dla Prawa budowlanego

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
            Generate a formal legal document in Polish using HTML.
            The document must be suitable for submission to court and ready for later conversion to PDF.

            CRITICAL FORMATTING RULES:
            1. return ONLY the HTML content. Do NOT include `<html>`, `<head>`, `<body>`, or `<!DOCTYPE>` tags.
            2. Do NOT use `<style>` blocks. You MUST use inline `style="..."` attributes for ALL styling.
            3. Use a simple clean layout.
            4. Use `<div>` or `<p>` tags for structure.

            Content rules:
            1. Use the value of "name" as the exact document title, centered and bold.
            2. The main body of the document must contain the data from "questions", written in proper legal and formal style. Each {"question": "answer"} should be converted into a coherent paragraph.
            3. The document layout and appearance must follow the structure described in "requirements". Treat "requirements" as layout and formatting guidelines (e.g., margins, sections, titles).
            4. IMPORTANT: Do NOT include any citations, legal bases, or ELI URIs in the generated document content.

            LEGAL BASIS USAGE (IMPORTANT):
            - The "base" field contains ELI URIs with links to legal documents on eli.gov.pl.
            - These links are provided ONLY as reference for you to understand the legal framework.
            - DO NOT include a "Podstawa prawna" section in the generated document.
            - DO NOT list or cite the legal bases (ELI URIs) or their links in the document output.
            - Use your knowledge of these legal acts to ensure correct legal terminology, proper phrasing, and accurate article references where naturally appropriate in the document text.
            - The legal basis information helps you write legally accurate content, but should NOT appear as a separate section or explicit citation in the output.

            TEMPLATE DOCUMENTS USAGE:
            If a "TEMPLATE DOCUMENTS" section is provided below, these are reference documents that serve as:
            - Visual and structural guides for the document you are generating
            - Examples of proper formatting, section organization, and layout conventions
            - Reference for the expected document structure and professional appearance
            
            When template documents are provided:
            - Match the general structure and section organization of the templates
            - Follow similar formatting patterns (headers, paragraphs, spacing)
            - Use the templates as a guide for professional legal document presentation
            - Fill in the actual data from "questions" while maintaining the template's structural approach
            - The templates represent the expected output format - generate a document that would look similar in structure

            Additional notes:
            - Use only formal legal language, no casual or informal tone.
            - Do not include any explanation, metadata, or comments.
            - Output only the final document content HTML fragment.

            JSON input:

             %s
            """;

    public static final String EN_EXTRACT_LEGAL_BASIS = """
                Your task is to identify the CORRECT and RELEVANT legal basis (podstawa prawna) for a document that the user wants to create.

                CRITICAL INSTRUCTIONS:
                1. Carefully analyze what TYPE of document the user wants to create
                2. Match the document type to the SPECIFIC Polish legal act that governs that area
                3. DO NOT guess or hallucinate ELI URIs - only use URIs you are confident exist
                4. The ELI URI format is: eli/DU/{year}/{position}/ogl - where position is the "poz." number, NOT the journal number "nr"

                Respond strictly in JSON format:
                {
                    "base": ["eli/DU/rok/pozycja/ogl"],
                    "reasoning": "Brief explanation of why these legal bases apply"
                }

                ELI URI FORMAT:
                eli/{dziennik}/{rok}/{pozycja}/{ogl|tj|uj}

                Where:
                - dziennik: DU (Dziennik Ustaw), MP (Monitor Polski)
                - rok: year of ORIGINAL publication
                - pozycja: the "poz." position number (NOT the "nr" journal number!)
                - ogl = original text, tj = consolidated text

                VERIFIED LEGAL ACTS BY DOMAIN (use ONLY these URIs):

                Civil Law:
                - "eli/DU/1964/93/ogl" - Kodeks cywilny (Civil Code)
                - "eli/DU/1964/296/ogl" - Kodeks postępowania cywilnego (Civil Procedure Code)

                Family Law:
                - "eli/DU/1964/59/ogl" - Kodeks rodzinny i opiekuńczy (Family and Guardianship Code)

                Criminal Law:
                - "eli/DU/1997/553/ogl" - Kodeks karny (Criminal Code)
                - "eli/DU/1997/555/ogl" - Kodeks postępowania karnego (Criminal Procedure Code)

                Administrative Law:
                - "eli/DU/1960/168/ogl" - Kodeks postępowania administracyjnego (Administrative Procedure Code)

                Real Estate & Geodesy:
                - "eli/DU/1989/163/ogl" - Prawo geodezyjne i kartograficzne (Geodesy and Cartography Law)
                - "eli/DU/1997/741/ogl" - Ustawa o gospodarce nieruchomościami (Real Estate Management Act)

                Labor Law:
                - "eli/DU/1974/141/ogl" - Kodeks pracy (Labor Code)

                Tax Law:
                - "eli/DU/1997/926/ogl" - Ordynacja podatkowa (Tax Ordinance)

                Construction:
                - "eli/DU/1994/414/ogl" - Prawo budowlane (Construction Law)

                MATCHING RULES:
                - For geodesy/surveying/maps/cartography/land measurement documents → use eli/DU/1989/163/ogl (Prawo geodezyjne)
                - For divorce/alimony/custody/marriage documents → use eli/DU/1964/59/ogl (Kodeks rodzinny)
                - For contracts/obligations/damages/civil matters → use eli/DU/1964/93/ogl (Kodeks cywilny)
                - For civil court proceedings/lawsuits → use eli/DU/1964/296/ogl (KPC)
                - For administrative requests/permits/appeals → use eli/DU/1960/168/ogl (KPA)
                - For real estate transactions/property management → use eli/DU/1997/741/ogl (Gospodarka nieruchomościami)
                - For employment/labor matters → use eli/DU/1974/141/ogl (Kodeks pracy)
                - For criminal matters → use eli/DU/1997/553/ogl (Kodeks karny)
                - For criminal proceedings → use eli/DU/1997/555/ogl (KPK)
                - For tax matters → use eli/DU/1997/926/ogl (Ordynacja podatkowa)
                - For construction/building permits → use eli/DU/1994/414/ogl (Prawo budowlane)

                These URIs will be verified against https://eli.gov.pl/ API. Only return URIs you are confident are correct.

                Return only the JSON data. Do not include any explanations outside the JSON structure.
            """;

    public static final String EN_GENERATE_QUESTIONS = """
                Your task is to prepare structured input data to generate a document based on a graphic template (form image) that will be provided as an attachment.
                All in Polish.

                The legal basis for this document has already been determined: %s

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
                    "base": %s,
                    "name": "suggestedApplicationName",
                    "requirements": ["documentRequirement1", "documentRequirement2"]
                }

                Step-by-step instructions:
                1. Analyze the image (template) to determine which fields and data points are required.
                2. Extract all data fields as user-facing questions and list them under `questions`.
                3. For each question, assign a unique `id` and determine its `type`.
                4. Use `PERSON`, `COMPANY`, `COURT`, or `LAW_FIRM` if the question specifically asks for details of such a legal entity. Use `ENTITY` if it's generic but clearly about a legal entity. Otherwise use `TEXT`.
                5. Include questions for personal data such as case reference number or mother’s maiden name **only if** they are required for this specific type of form.
                6. Use the pre-verified legal basis provided above in the `base` field.
                7. In the `name` field, provide a suggested name for the application or request.
                8. In `requirements`, list the necessary supporting documents or attachments.

                Return only the JSON data. Do not include any explanations, comments, or additional content.
                Remember that data like applicant, date it was created and legal entity to which the application is directed must be provided.
                There is no need to signature space.
            """;
    public static final String EN_CHAT_WITH_DOCUMENT = """
            You are a legal document editing assistant. Your task is to modify a legal document (HTML) based on the user's request.

            INPUT:
            - Current Document Content (HTML)
            - User Request (text)

            OUTPUT:
            Return a JSON object with the following structure:
            {
                "proposedContent": "The complete modified HTML content",
                "updateData": { // Optional: Structured data if the change is better represented as data
                    "name": "Document Title",
                    "questions": { "questionId": "answer" },
                    "base": ["eli/DU/..."],
                    "requirements": ["requirement"]
                },
                "explanation": "A clear explanation of what changed and why, in Polish",
                "legalBasis": ["eli/DU/..."] // List of ELI URIs representing the legal basis for this change
            }

            RULES:
            1. Return ONLY the JSON object. logic should be in Polish.
            2. You can provide either `proposedContent` (HTML) or `updateData` (JSON), or both.
            3. If `proposedContent` is provided, it must be the COMPLETE document HTML, not just a diff.
            4. Maintain the existing HTML structure and styling (inline styles).
            5. `explanation` should be professional, concise, and explain the legal reasoning if applicable.
            6. `legalBasis` should include ELI URIs if specific laws justify the change.
            7. If `updateData` is used, follow the format used for document generation (name, questions, base, requirements).
            8. Do not remove placeholders or essential structure unless requested.
            9. Ensure the HTML is valid and clean.
            """;
}
