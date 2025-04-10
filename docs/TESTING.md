# Guide de Test et Validation - CashRuler

## 📚 Vue d'ensemble

CashRuler dispose d'une suite complète de tests et d'outils de validation pour assurer la qualité, la performance et la sécurité de l'application.

## 🎯 Types de Tests

### 1. Tests Unitaires
- Localisation: `app/src/test/`
- Exécution: `./gradlew testDebugUnitTest`
- Couvre: Repositories, ViewModels, Workers

### 2. Tests d'Intégration
- Localisation: `app/src/androidTest/`
- Exécution: `./gradlew connectedDebugAndroidTest`
- Couvre: Database, Navigation, End-to-End flows

### 3. Tests de Performance
- Localisation: `app/src/androidTest/java/com/cashruler/benchmark/`
- Exécution: `./gradlew :app:benchmarkAppStart`
- Mesure: Temps de démarrage, animations, utilisation mémoire

### 4. Tests d'Accessibilité
- Localisation: `app/src/androidTest/java/com/cashruler/validation/`
- Vérifie: TalkBack support, tailles de clic, contrastes
- Exécution via ValidationSuite

### 5. Tests de Sécurité
- Localisation: `app/src/androidTest/java/com/cashruler/validation/`
- Vérifie: Chiffrement, permissions, stockage sécurisé
- Exécution via ValidationSuite

## 🛠 Outils de Validation

### ValidationSuite
Suite complète qui exécute tous les tests et validations :
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cashruler.validation.ValidationSuite
```

### Script d'Automatisation
```bash
./scripts/run_validation.sh
```
Exécute tous les tests et génère des rapports détaillés.

## 📊 Rapports

Les rapports sont générés dans les emplacements suivants :
- Tests unitaires: `app/build/reports/tests/`
- Couverture: `app/build/reports/coverage/`
- Lint: `app/build/reports/lint/`
- Performance: `app/build/reports/benchmarks/`
- Validation: `validation-results/`

## 🔍 Validation des Composants

### 1. Données
```kotlin
val dataValidator = DataValidator()
val result = dataValidator.validateExpense(expense)
```

### 2. Performance
```kotlin
val performanceValidator = PerformanceValidator()
val result = performanceValidator.validateAnimation(frameTimesMs)
```

### 3. Accessibilité
```kotlin
val accessibilityValidator = AccessibilityValidator(composeTestRule)
val results = accessibilityValidator.validateScreen("Dashboard")
```

### 4. Sécurité
```kotlin
val securityValidator = SecurityValidator()
val results = securityValidator.validateSecurity()
```

## 🚀 CI/CD

L'intégration continue est configurée via GitHub Actions :
- Tests automatiques sur chaque PR
- Génération de rapports de couverture
- Vérification des performances
- Publication automatique des releases

## 📝 Bonnes Pratiques

1. **Tests Unitaires**
   - Testez chaque fonction de manière isolée
   - Utilisez des mocks pour les dépendances
   - Visez une couverture > 80%

2. **Tests d'Intégration**
   - Testez les flux complets
   - Vérifiez les interactions entre composants
   - Utilisez des données réalistes

3. **Tests de Performance**
   - Mesurez sur plusieurs itérations
   - Testez avec différentes tailles de données
   - Définissez des seuils clairs

4. **Tests d'Accessibilité**
   - Vérifiez la compatibilité TalkBack
   - Testez les contrastes de couleur
   - Validez les tailles de cible de clic

5. **Tests de Sécurité**
   - Vérifiez le chiffrement des données
   - Testez les permissions
   - Validez la sécurité des sauvegardes

## 🔧 Maintenance

1. **Mise à jour des Tests**
   - Maintenez les tests à jour avec les changements de code
   - Revoyez régulièrement les seuils de performance
   - Mettez à jour les scénarios de test

2. **Analyse des Résultats**
   - Examinez les rapports après chaque exécution
   - Surveillez les tendances de performance
   - Corrigez rapidement les échecs

3. **Documentation**
   - Documentez les nouveaux tests
   - Mettez à jour les seuils et configurations
   - Maintenez ce guide à jour

## ⚠️ Résolution des Problèmes

1. **Tests en Échec**
   - Consultez les logs détaillés
   - Vérifiez les changements récents
   - Isolez le problème avec des tests unitaires

2. **Problèmes de Performance**
   - Utilisez Android Profiler
   - Analysez les rapports de benchmark
   - Identifiez les goulots d'étranglement

3. **Problèmes de CI**
   - Vérifiez les logs GitHub Actions
   - Testez localement avec les mêmes paramètres
   - Validez les configurations d'environnement
