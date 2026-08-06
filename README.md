# ledger

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).

## Building & Running
To build or run the project, use one of the following tasks:

| Task | Description |
|------|-------------|
| `./gradlew test`    | Run the tests     |
| `./gradlew build`   | Build the project |
| `./gradlew run`     | Run the server    |

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### API documentation

The OpenAPI document is generated from the live routing tree at startup — every route a
`RestController` registers is included without being listed anywhere.

| Path | Description |
|------|-------------|
| `/` | Rendered HTML documentation |
| `/openapi.json` | The OpenAPI document itself, as JSON |

The rendered HTML is written to `docs/` on startup; it is generated output, not source.

### Port

The server listens on `8080` by default. Override it with the `PORT` environment variable:

```sh
PORT=9090 ./gradlew run
```

A non-numeric `PORT` fails at startup rather than falling back to the default, so a typo can't
leave the server listening somewhere unexpected.
