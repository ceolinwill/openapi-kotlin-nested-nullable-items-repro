# Kotlin client: a nested list drops its items' nullability

Reported in https://github.com/OpenAPITools/openapi-generator/issues/25471.

A list of lists whose items `$ref` a nullable schema is generated without the `?` on the inner items. `column` (a list of `Note`) becomes `List<Note?>`, but `grid` (a list of lists of `Note`) becomes `List<List<Note>>`, so a `null` inside `grid` can't be decoded. The same happens when the items are an inline nullable `anyOf`, like a table cell that's a string, a number or `null`: the inline resolver turns it into a `$ref`.

## Reproduction

Needs Node and a JDK 17 or newer.

```sh
npm install
npm run repro
```

Output with openapi-generator 7.26.0:

```
decoded: JsonDecodingException: Unexpected JSON token at offset 26: Expected start of the object '{', but had 'n' instead at path: $.grid[0][0]
```

`npm run repro` generates the client from [openapi.yaml](openapi.yaml) (`-g kotlin`, `serializationLibrary=kotlinx_serialization`), then compiles only the generated models with [Main.kt](src/main/kotlin/Main.kt), which decodes `{"column":[null],"grid":[[null]]}`.

## Expected

`grid` is `List<List<Note?>>`, and:

```
decoded: Board(column=[null], grid=[[null]])
```

## Actual

```kotlin
val column: kotlin.collections.List<Note?>,
val grid: kotlin.collections.List<@Contextual kotlin.collections.List<Note>>
```

`DefaultCodegen.fromProperty` reads `nullable` through a `$ref` (`ModelUtils.isNullable(referencedSchema)`), so the inner items' `CodegenProperty.isNullable` is true, and `data_class_req_var.mustache` adds the `?` for one level (`{{{items.dataType}}}{{#items.isNullable}}?{{/items.isNullable}}`). But the inner list's own `dataType` comes from [`AbstractKotlinCodegen.getItemsTypeDeclaration`](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AbstractKotlinCodegen.java#L421-L425), which checks only `items.getNullable()` on the bare `$ref` schema and never looks at the schema it references.

## Environment

- openapi-generator 7.26.0 (latest release; `master` has the same code), through `@openapitools/openapi-generator-cli` 2.42.0
- Kotlin 2.4.0, kotlinx-serialization-json 1.11.0, Gradle 9.8.2, JDK 25
- macOS 27
