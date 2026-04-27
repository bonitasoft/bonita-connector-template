# CLAUDE.md

## Project Overview

This is an official Bonita connector for the Bonita BPM platform.

**Technology Stack:**
- Java 17
- Maven
- Bonita Runtime 10.2.0+

## Build and Test Commands

```bash
# Build all (skip tests)
./mvnw clean package

# Build with tests
./mvnw clean verify

# Run unit tests only
./mvnw test

# Run a single test class
./mvnw test -Dtest=MyConnectorTest
```

## Commit Message Format

Use conventional commits:
```
type(scope): description
```

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `perf`

## Release Process

Releases are managed via GitHub Actions:
1. Run the "Release" workflow with the target version
2. Optionally set `update_marketplace: true` to publish to the Bonita marketplace

## Connector Implementation Pattern

Every operation class extends an `AbstractMistralOcrConnector`-style base that owns the lifecycle around `doExecute()`. The base:

1. Initialises `success=false` and `errorMessage=""`.
2. Calls a subclass-specific `initializeOutputs()` hook that pre-fills every operation-specific output with a neutral default (empty string, empty list, empty map, 0).
3. Wraps `doExecute()` in a try/catch that flips `success=true` on the happy path and writes the message to `errorMessage` on failure.

Skeleton:

```java
public abstract class AbstractXxxConnector extends AbstractConnector {
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        setOutputParameter(OUTPUT_SUCCESS, false);
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");
        initializeOutputs();
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (XxxException e) {
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            setOutputParameter(OUTPUT_ERROR_MESSAGE,
                "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws XxxException;
    /**
     * Pre-fill every output declared in the .def with a neutral default so
     * a .proc mapping an output never crashes with SExpressionEvaluationException.
     */
    protected abstract void initializeOutputs();
}
```

Each subclass implements `initializeOutputs()`:

```java
@Override
protected void initializeOutputs() {
    setOutputParameter(OUTPUT_EXTRACTED_TEXT, "");
    setOutputParameter(OUTPUT_PAGES, java.util.List.of());
    setOutputParameter(OUTPUT_PAGE_COUNT, 0);
    setOutputParameter(OUTPUT_TOKENS_USED, 0);
}
```

## Common Pitfalls

These have all bitten real connectors. Each one has an associated test pattern in the next section.

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | `<include>*:jar</include>` typo in `assembly.xml` ships zips without the connector JAR → `NoClassDefFoundError` at runtime | Use `<include>*.jar</include>` (dot, not colon). Verify by extracting one zip and grepping for the connector JAR. |
| 2 | Wrong API endpoint for the operation (e.g. routing a PDF to a vision endpoint) → 422 | Read the API reference. PDFs typically need OCR-style endpoints, not vision; vision endpoints generally only accept image MIMEs. |
| 3 | Custom JSON body shape that doesn't match the API contract | Always copy the exact request body shape from the API docs. Use a discriminated union (`type` + payload) where the API expects one. |
| 4 | Silent override of user-provided inputs (`model`, `baseUrl`...) | Never rewrite a user input. Throw `ConnectorValidationException` if incompatible. |
| 5 | Outputs declared in `.def` but only set in some code paths → `SExpressionEvaluationException` | See `initializeOutputs()` pattern above. |
| 6 | API uses 0-indexed page numbers but the connector forwards 1-indexed user input | Document 1-indexed in `.def`/`.properties`, subtract 1 inside the connector before sending. |
| 7 | Groovy script in a `.proc` returns a `Map.toString()` instead of valid JSON | Use `groovy.json.JsonOutput.toJson(map)`. |
| 8 | Connector palette icon at 64x64 (cropped/oversized in Studio) | Use 16x16 for the palette `.def`. Keep 64x64 as `<connector>-logo.png` for marketplace listings. |
| 9 | New widget added to `.def` shows up unnamed in Studio | Add `widgetId.label=` and `widgetId.description=` to the `.properties`. |

## Testing Pattern

Three layers, all complementary:

### 1. Unit tests with a mocked client (regression coverage)

Standard mocked-client pattern. Use a fresh connector instance per test (see `connector-testing-gotchas.md` § AbstractConnector.setInputParameters Uses putAll).

### 2. WireMock tests asserting the **request body** (API contract coverage)

Mocked clients do not catch "wrong body shape" bugs. For every external HTTP call, add at least one test that:

```java
@Test
void operation_shouldSendBodyMatchingApiContract() throws Exception {
    wireMock.stubFor(post(urlEqualTo("/v1/endpoint"))
            .willReturn(aResponse().withStatus(200).withBody("{\"ok\":true}")));

    // call the real client (no mocks)
    client.operation(config);

    wireMock.verify(postRequestedFor(urlEqualTo("/v1/endpoint"))
            .withRequestBody(matchingJsonPath("$.discriminator", equalTo("expectedValue"))));
}
```

### 3. Smoke test against the real API (gated by env var)

```java
@EnabledIfEnvironmentVariable(named = "XXX_API_KEY", matches = ".+")
class XxxSmokeTest {
    @Test
    void shouldExtractFromPublicFixture() throws Exception {
        // call real production API with a known fixture
        // assert structural properties only (not exact text)
    }
}
```

CI skips when the env var is absent. Run locally before the release PR.

### Optional: standalone mock server for demos

A `XxxMockServer` `main` class that boots WireMock on `localhost:8089` with canned responses. Bonita Studio points the connector's `baseUrl` to `http://localhost:8089/v1` and runs a real `.proc` against it. Saves time during PoCs and customer demos.
