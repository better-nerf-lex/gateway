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
            You are a senior Polish legal drafting assistant. Generate a complete, formal legal document in Polish as an HTML fragment.
            The document must be suitable for professional review and later conversion to PDF.

            Critical output rules:
            1. Return ONLY the HTML content. Do NOT include `<html>`, `<head>`, `<body>`, or `<!DOCTYPE>` tags.
            2. Do NOT use `<style>` blocks. You MUST use inline `style="..."` attributes for ALL styling.
            3. Do NOT use markdown fences, explanations, comments, metadata, or alternate drafts.
            4. Use a simple A4-friendly layout with readable spacing and professional typography.
            5. Use semantic HTML fragments built from `<div>`, `<p>`, `<h1>`, `<h2>`, `<strong>`, `<ol>`, `<ul>`, and `<li>`.

            Drafting rules:
            1. Use the value of `name` as the exact document title, centered and bold.
            2. Convert `questions` and answers into coherent legal prose, not a questionnaire recap.
            3. Preserve every concrete fact supplied by the user: names, addresses, dates, amounts, case numbers, courts, companies, and requested relief.
            4. Do not invent missing facts. If a legally necessary fact is missing, include a neutral placeholder in natural Polish only when unavoidable, e.g. `[uzupełnić datę]`.
            5. Avoid casual language, generic filler, and repeated sentences.
            6. Treat `requirements` as drafting and layout requirements, including attachments, requested sections, or special formatting.
            7. If case context or attached context files are provided, use them as factual context and keep facts consistent across the document.
            8. If templates are provided, mirror their section order, spacing style, and professional structure while using the user's actual data.
            9. End with only sections that are appropriate for this document type, such as requests, justification, attachments, or closing/signature area when expected.

            LEGAL BASIS USAGE (IMPORTANT):
            - Legal basis material may be provided as reference context outside the JSON input.
            - It is metadata for internal drafting only and is saved separately by the application.
            - NEVER include a "Podstawa prawna", "Podstawy prawne", or "Legal basis" section in the generated document.
            - NEVER list legal bases, ELI URIs, eli.gov.pl links, or statute lists at the end of the document.
            - Use legal basis only to choose correct legal terminology, document structure, and article references where naturally appropriate in the document text.
            - The legal basis information helps you write legally accurate content, but should NOT appear as a separate section, appendix, metadata block, or explicit source list in the output.

            Quality checklist before responding:
            - The title is present and matches `name`.
            - The document contains all supplied answers unless they are irrelevant duplicates.
            - The output is long enough to be useful, with a clear structure and formal justification.
            - The output contains no legal-basis section, no ELI URI, and no eli.gov.pl link.
            - The output has no markdown, no full-page wrapper tags, no `<style>` block, and no unexplained placeholders.
            """;

    public static final String EN_REPAIR_GENERATED_DOCUMENT = """
            You are repairing a generated Polish legal document that failed quality checks.

            Return only a corrected HTML fragment.
            Preserve the user's facts exactly.
            Do not add markdown fences, comments, `<html>`, `<head>`, `<body>`, `<!DOCTYPE>`, or `<style>` blocks.
            Use inline styles only.
            Remove generic filler and unresolved placeholders unless the original input truly lacks a legally required fact.
            Remove any legal-basis/source section, ELI URI, eli.gov.pl link, or statute list appended as metadata.
            Ensure the final document is complete, formal, structured, and suitable for professional review before PDF conversion.
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
                Your task is to prepare the FIRST PASS of structured input data for generating a legal document.
                All questions must be in Polish.

                The legal basis for this document has already been determined: %s

                Respond strictly in JSON format, using the following schema:
                {
                    "questions": [
                        {
                            "id": "unique_id_1",
                            "text": "question_text_1",
                            "type": "TEXT | PERSON | COMPANY | COURT | LAW_FIRM | ENTITY",
                            "followup": true
                        },
                        ...
                    ],
                    "base": %s,
                    "name": "suggestedApplicationName",
                    "requirements": ["documentRequirement1", "documentRequirement2"]
                }

                Step-by-step instructions:
                1. Generate broad screening questions first. Do not ask every nested detail yet.
                2. Prefer generic questions that determine whether a branch applies, e.g. whether there are children, whether there is a contract, whether payment was made, whether there is a court case, whether there are attachments.
                3. Ask for core facts that are always necessary for the document, such as applicant/party, recipient/court/authority, dates, amount, case number if applicable, and requested outcome.
                4. Use `PERSON`, `COMPANY`, `COURT`, or `LAW_FIRM` if the question specifically asks for details of such a legal entity. Use `ENTITY` if it's generic but clearly about a legal entity. Otherwise use `TEXT`.
                5. Set `followup` to true only when this question is a broad branch/screening question whose positive answer may require later details. Examples: children exist, contract exists, payment was made, damage happened, attachments exist.
                6. Set `followup` to false when the answer is already concrete enough or the question asks for always-required data such as applicant, recipient, date, amount, case number, or requested outcome.
                7. Each first-pass question must ask exactly one thing. Do not combine a screening question with a conditional detail request.
                8. Do not ask conditional deep follow-up questions in this first pass. For example, ask "Czy strony mają dzieci?" now with `followup: true`, but do not ask for each child’s detailed data until the user confirms that children exist.
                9. Forbidden first-pass question patterns include: "Jeśli tak, ...", "Jeżeli tak, ...", "W przypadku odpowiedzi twierdzącej, ...", "Czy ...? Jeśli tak, podaj ...", and any equivalent wording that asks for branch details upfront.
                10. If a detail is needed only after a yes/positive answer, omit that detail from the first pass. The follow-up analyzer will ask it later only when the answer makes it relevant.
                11. Use the pre-verified legal basis provided above in the `base` field.
                12. In the `name` field, provide a suggested name for the application or request.
                13. In `requirements`, list only obvious supporting documents or attachments. More precise requirements may be updated after follow-up analysis.

                Return only the JSON data. Do not include any explanations, comments, or additional content.
                Remember that data like applicant, date it was created and legal entity to which the application is directed must be provided.
                There is no need to signature space.
            """;

    public static final String EN_ANALYZE_FOLLOW_UP_QUESTIONS = """
            You analyze a Polish legal-document questionnaire after the user answered the current questions.

            Goal:
            Decide whether the existing answers require additional concrete follow-up questions before document generation.
            This is for unknown document types, so reason from the user's request, current questions, answers, legal basis, and requirements.
            The `answers` object is keyed by the zero-based index of the corresponding item in `questions`.

            Return strictly JSON in this schema:
            {
                "questions": [
                    {
                        "id": "unique_follow_up_id",
                        "text": "follow_up_question_in_polish",
                        "type": "TEXT | PERSON | COMPANY | COURT | LAW_FIRM | ENTITY",
                        "followup": false
                    }
                ],
                "base": ["eli/DU/..."],
                "name": "documentName",
                "requirements": ["updatedRequirement1"]
            }

            Rules:
            1. Analyze only questions where `followup` is true. Add follow-up questions only when that answer creates a need for more concrete data.
            2. Do not repeat questions already asked. Do not ask for information already answered.
            3. If a broad answer is negative or says the branch does not apply, do not ask branch details.
            4. If the user confirms a branch applies, ask for the missing concrete data needed to draft correctly.
               Examples:
               - If there are children, ask for each child's name, birth date, residence, custody/contact/alimony details as relevant.
               - If money is owed, ask for amount, due date, payment basis, debtor, creditor, interest, and evidence if missing.
               - If a contract exists, ask for parties, date, object, key clauses, breach, and evidence if missing.
               - If there is a court/administrative case, ask for court/authority, case number, parties, dates, and requested action if missing.
               - If attachments are mentioned, ask which documents exist and what they prove if missing.
            5. Prefer a small number of high-value follow-ups. Avoid overwhelming the user.
            6. Keep questions generic enough for the UI, but concrete enough to collect draftable legal facts.
            6a. Set `followup` to false on every question you return. This follow-up pass must not create another follow-up chain.
            7. Update `requirements` based on all known answers. Keep existing valid requirements and add newly implied attachments.
            8. Keep `base` as the best known legal basis. Do not hallucinate ELI URIs; keep existing base if unsure.
            9. If no more follow-ups are needed, return `"questions": []` with updated `base`, `name`, and `requirements`.

            Return only JSON. No markdown, no comments, no explanation.
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
