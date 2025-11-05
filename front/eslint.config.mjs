// @ts-check
import eslint from "@eslint/js"
import stylistic from "@stylistic/eslint-plugin"
import angular from "angular-eslint"
import importEslint from "eslint-plugin-import"
import tseslint from "typescript-eslint"

export default tseslint.config(
  {
    ignores: [
      "**/node_modules/**",
      "dist/**",
      "projects/**/dist/**",
      "src/polyfills.ts",
      "src/test.ts",
    ],
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      stylistic.configs["recommended"],
      ...angular.configs.tsRecommended,
      importEslint.flatConfigs.typescript,
    ],
    settings: {
      // Résout les imports TypeScript pour tout le monorepo
      "import/resolver": {
        typescript: {
          project: [
            "./tsconfig.json",
            "./projects/*/tsconfig*.json",
          ],
          alwaysTryTypes: true,
        },
      },
      // Tout ce qui commence par "~" est "internal"
      "import/internal-regex": "^~",
    },
    processor: angular.processInlineTemplates,
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@angular-eslint/directive-selector": [
        "error",
        { type: "attribute", prefix: "app", style: "camelCase" },
      ],
      "@angular-eslint/component-selector": [
        "error",
        { type: "element", prefix: "app", style: "kebab-case" },
      ],
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrorsIgnorePattern: "^_",
        },
      ],
      semi: ["error", "never"],

      // Ordonne les imports: externals (avec @angular et primeng en tête), puis internes (~...), puis relatifs
      "import/order": [
        "error",
        {
          groups: ["builtin", "external", "internal", "parent", "sibling", "index"],
          "newlines-between": "always",
          alphabetize: { order: "asc", caseInsensitive: true },
          pathGroups: [
            { pattern: "@angular/**", group: "external", position: "before" },
            { pattern: "primeng?(/**)", group: "external", position: "before" },
            { pattern: "@quezap/**", group: "internal", position: "after" },
          ],
          pathGroupsExcludedImportTypes: ["builtin"],
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      // Règles recommandées pour les templates HTML d'Angular
      ...angular.configs.templateRecommended,

      // Règles recommandées pour l'accessibilité dans les templates
      ...angular.configs.templateAccessibility,
    ],
    rules: {
      // Exemples de règles que vous pouvez ajouter pour le HTML
      // "@angular-eslint/template/no-negated-async": "error"
    }
  },
  {
    files: ["**/*.ts"],
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "quizz",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "quizz",
          style: "kebab-case",
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    rules: {},
  }
)
