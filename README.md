# 📚 Documentación: Lógica de Funcionamiento - Parser de Gramáticas Formales

## 🎯 Descripción General

Esta aplicación JavaFX implementa un analizador sintáctico (parser) para gramáticas formales de **Tipo 2 (Libres de Contexto)** y **Tipo 3 (Regulares)** según la jerarquía de Chomsky. Permite crear, cargar, editar gramáticas y validar cadenas de entrada mediante diferentes algoritmos de parsing.

---

## 🏗️ Arquitectura del Sistema

### 📂 Estructura de Paquetes

```
org.example/
├── Main.java                    # Punto de entrada de la aplicación
├── model/                       # Modelos de datos (Entidades)
│   ├── Grammar.java            # Representación de una gramática formal
│   ├── Production.java         # Reglas de producción (A → α)
│   └── DerivationTree.java     # Árbol de derivación para trazabilidad
├── parser/                      # Algoritmos de análisis sintáctico
│   ├── Parser.java             # Interfaz común para parsers
│   ├── ParseResult.java        # Resultado del proceso de parsing
│   ├── ParserFactory.java      # Factory pattern para crear parsers
│   ├── Type2Parser.java        # Parser recursivo descendente (CFG)
│   └── Type3Parser.java        # Simulador de autómata (Regular)
├── generator/                   # Generación de cadenas válidas
│   └── StringGenerator.java    # BFS para generar cadenas de la gramática
├── persistence/                 # Persistencia de datos
│   └── GrammarPersistence.java # Serialización JSON de gramáticas
└── view/                       # Interfaz de usuario
    └── JavaFXApp.java          # Aplicación JavaFX principal
```

---

## 🔧 Componentes Principales

### 1️⃣ **Modelo de Datos (Model Layer)**

#### 🔹 **Grammar.java**
Representa una gramática formal G = (N, T, P, S, Type):

```java
public class Grammar {
    private Set<String> nonTerminals;    // N - Símbolos no terminales
    private Set<String> terminals;       // T - Símbolos terminales  
    private List<Production> productions; // P - Reglas de producción
    private String startSymbol;          // S - Símbolo inicial
    private GrammarType type;            // Tipo de gramática (2 o 3)
}
```

**Funcionalidades:**
- ✅ Validación de integridad de la gramática
- ✅ Verificación de que el símbolo inicial esté en N
- ✅ Validación de que N ∩ T = ∅ (conjuntos disjuntos)
- ✅ Verificación de producciones válidas según el tipo

#### 🔹 **Production.java**
Representa una regla de producción A → α:

```java
public class Production {
    private String left;          // Lado izquierdo (A)
    private List<String> right;   // Lado derecho (α) como lista de símbolos
}
```

**Lógica de Parsing:**
- **Símbolos simples:** `"a"` → `["a"]`
- **Símbolos múltiples:** `"E + T"` → `["E", "+", "T"]`
- **Epsilon:** `"ε"` → `["ε"]` (cadena vacía)

#### 🔹 **DerivationTree.java**
Estructura de árbol para rastrear derivaciones:

```java
public class DerivationTree {
    private String symbol;                    // Símbolo del nodo
    private List<DerivationTree> children;    // Hijos (derivaciones)
    private boolean isTerminal;               // ¿Es terminal?
}
```

---

### 2️⃣ **Capa de Parsing (Parser Layer)**

#### 🔹 **Type2Parser.java - Parser Recursivo Descendente**

**Algoritmo Principal:**
```
parseRecursiveImproved(tokens, symbolStack, position, visitedStates)
├── Si symbolStack vacía && tokens consumidos → ACEPTAR
├── Si límites excedidos → RECHAZAR
├── Pop symbol del stack
├── Si symbol es terminal:
│   ├── Si coincide con token actual → avanzar
│   └── Sino → RECHAZAR
└── Si symbol es no-terminal:
    ├── Para cada producción con left = symbol:
    │   ├── Verificar detección de ciclos
    │   ├── Push símbolos right al stack (orden inverso)
    │   ├── Llamada recursiva
    │   └── Si exitosa → ACEPTAR
    └── Si ninguna producción funciona → RECHAZAR
```

**Características Avanzadas:**
- 🔄 **Detección de Ciclos:** Evita bucles infinitos con `HashSet<String> visitedStates`
- 📏 **Límites Adaptativos:** `maxDepth` y `maxSteps` basados en longitud de entrada
- 🎯 **Backtracking Inteligente:** Prueba todas las producciones posibles
- ✅ **Validación Estricta:** Solo acepta si consume TODA la entrada

#### 🔹 **Type3Parser.java - Simulador de Autómata**

**Algoritmo de Simulación:**
```
parse(input)
├── currentStates = {startSymbol}
├── Para cada símbolo en input:
│   ├── newStates = ∅
│   ├── Para cada estado actual:
│   │   ├── Buscar producciones aplicables
│   │   └── Agregar nuevos estados alcanzables
│   └── currentStates = newStates
└── ¿Algún estado actual es final? → ACEPTAR/RECHAZAR
```

---

### 3️⃣ **Generación de Cadenas (Generator Layer)**

#### 🔹 **StringGenerator.java - Búsqueda BFS**

**Algoritmo BFS (Breadth-First Search):**
```
generateStrings(grammar, maxLength, maxCount)
├── queue = [startSymbol]
├── results = []
├── Mientras queue no vacía && |results| < maxCount:
│   ├── current = queue.dequeue()
│   ├── Si current es terminal && |current| ≤ maxLength:
│   │   └── results.add(current)
│   ├── Para cada producción aplicable:
│   │   ├── newString = aplicarProducción(current, producción)
│   │   ├── Si |newString| ≤ maxLength && no visitado:
│   │   │   └── queue.enqueue(newString)
│   └── Marcar current como visitado
└── return results
```

**Ventajas del BFS:**
- 🎯 Genera cadenas en orden de longitud (cortas primero)
- 🔄 Evita duplicados con `Set<String> visited`
- ⚡ Termina cuando alcanza el límite de cadenas o longitud

---

### 4️⃣ **Interfaz de Usuario (View Layer)**

#### 🔹 **JavaFXApp.java - Aplicación Principal**

**Estructura de la UI:**
```
BorderPane (root)
├── Top: MenuBar
│   ├── Archivo (Nuevo, Abrir, Guardar, Salir)
│   ├── Ejemplos (Gramáticas predefinidas)
│   └── Ayuda (Acerca de)
├── Center: SplitPane (3 paneles)
│   ├── Panel Izquierdo: Editor de Gramática
│   │   ├── TextArea para no-terminales
│   │   ├── TextArea para terminales  
│   │   ├── TextArea para producciones
│   │   └── ComboBox para símbolo inicial
│   ├── Panel Central: Parser de Cadenas
│   │   ├── TextField para entrada
│   │   ├── ComboBox para tipo de parser
│   │   └── TextArea para resultados
│   └── Panel Derecho: Generador de Cadenas
│       ├── Controles de configuración
│       └── ListView de cadenas generadas
└── Bottom: StatusBar
```

---

## 🧮 Algoritmos Clave

### 🔍 **Algoritmo de Parsing Tipo 2 (Detallado)**

```java
private boolean parseRecursiveImproved(List<String> tokens, Stack<String> symbolStack, 
                                     int position, Set<String> visitedStates) {
    
    // 1. Condiciones de terminación
    if (symbolStack.isEmpty()) {
        return position == tokens.size(); // Acepta solo si consumió toda la entrada
    }
    
    // 2. Verificar límites para evitar explosión combinatoria
    if (recursionDepth > maxDepth || steps > maxSteps) {
        return false;
    }
    
    // 3. Procesar símbolo actual
    String symbol = symbolStack.pop();
    steps++;
    
    // 4. Si es terminal: verificar coincidencia directa
    if (grammar.getTerminals().contains(symbol)) {
        if (position < tokens.size() && tokens.get(position).equals(symbol)) {
            return parseRecursiveImproved(tokens, symbolStack, position + 1, visitedStates);
        }
        return false;
    }
    
    // 5. Si es no-terminal: probar todas las producciones
    for (Production production : getProductionsFor(symbol)) {
        // 5.1 Detectar ciclos infinitos
        String stateKey = generateStateKey(symbolStack, position, production);
        if (visitedStates.contains(stateKey)) {
            continue; // Evitar bucle infinito
        }
        
        // 5.2 Crear nuevo contexto
        Stack<String> newStack = cloneStack(symbolStack);
        Set<String> newVisited = new HashSet<>(visitedStates);
        newVisited.add(stateKey);
        
        // 5.3 Expandir producción (push en orden inverso)
        List<String> rightSide = production.getRight();
        for (int i = rightSide.size() - 1; i >= 0; i--) {
            if (!rightSide.get(i).equals("ε")) {
                newStack.push(rightSide.get(i));
            }
        }
        
        // 5.4 Llamada recursiva con backtracking
        recursionDepth++;
        boolean success = parseRecursiveImproved(tokens, newStack, position, newVisited);
        recursionDepth--;
        
        if (success) {
            return true; // Backtracking exitoso
        }
    }
    
    return false; // Ninguna producción funcionó
}
```

### 🔄 **Algoritmo de Generación BFS (Detallado)**

```java
public List<String> generateStrings(Grammar grammar, int maxLength, int maxCount) {
    Queue<String> queue = new LinkedList<>();
    Set<String> visited = new HashSet<>();
    List<String> results = new ArrayList<>();
    
    // Inicializar con símbolo inicial
    queue.offer(grammar.getStartSymbol());
    visited.add(grammar.getStartSymbol());
    
    while (!queue.isEmpty() && results.size() < maxCount) {
        String current = queue.poll();
        
        // Si es cadena terminal válida, agregarla a resultados
        if (isTerminalString(current) && current.length() <= maxLength) {
            results.add(current.equals("ε") ? "" : current);
            continue;
        }
        
        // Expandir usando todas las producciones aplicables
        for (Production production : grammar.getProductions()) {
            List<String> expansions = applyProduction(current, production);
            
            for (String expansion : expansions) {
                if (expansion.length() <= maxLength && !visited.contains(expansion)) {
                    queue.offer(expansion);
                    visited.add(expansion);
                }
            }
        }
    }
    
    return results;
}
```

---

## 💾 Gestión de Datos

### 🔹 **Serialización JSON (GrammarPersistence.java)**

```java
// Estructura JSON para persistencia
{
  "nonTerminals": ["S", "A", "B"],
  "terminals": ["a", "b", "c"],
  "productions": [
    {"left": "S", "right": ["A", "B"]},
    {"left": "A", "right": ["a"]},
    {"left": "B", "right": ["b"]}
  ],
  "startSymbol": "S",
  "type": "TYPE_2"
}
```

**Funciones:**
- ✅ `saveGrammar(Grammar, File)`: Serialización a JSON
- ✅ `loadGrammar(File)`: Deserialización desde JSON
- ✅ Manejo robusto de errores de I/O

---

## 🎨 Características de la Interfaz

### 🎯 **Características Clave:**
- **🎨 Tema Oscuro:** Aplicado via CSS con overrides programáticos
- **📱 Responsive:** SplitPane redimensionable con pesos equilibrados
- **⚡ Tiempo Real:** Validación inmediata al editar gramáticas
- **🔄 Sincronización:** Cambios automáticos entre paneles
- **📋 Ejemplos Integrados:** Gramáticas predefinidas para aprendizaje

### 🎨 **Sistema de Estilos:**
```css
/* styles.css - Tema principal */
.root { -fx-base: #2b2b2b; }
.text-area, .text-field { -fx-text-fill: white; }
.menu-bar { -fx-background-color: #3c3c3c; }

/* Overrides programáticos para compatibilidad */
forceMenuStylesAggressive() {
    Platform.runLater(() -> {
        scene.getStylesheets().add("data:text/css," + encodeCSS(WHITE_MENU_STYLE));
    });
}
```

---

## 🧪 Ejemplos de Gramáticas Incluidas

### 1️⃣ **Expresiones Aritméticas (Tipo 2)**
```
N = {E, T, F}
T = {+, *, (, ), x}
P = {
    E → E + T | T
    T → T * F | F  
    F → ( E ) | x
}
S = E
```
**Cadenas válidas:** `x`, `x+x`, `x*x`, `(x+x)*x`

### 2️⃣ **Expresiones con Identificadores (Tipo 2)**
```
N = {E, T, F}
T = {+, *, (, ), id}
P = {
    E → E + T | T
    T → T * F | F
    F → ( E ) | id
}
S = E
```
**Cadenas válidas:** `id`, `id+id`, `id*id`, `(id+id)*id`

### 3️⃣ **Palíndromos Pares (Tipo 2)**
```
N = {S}
T = {a, b}
P = {
    S → a S a | b S b | ε
}
S = S
```
**Cadenas válidas:** `ε`, `aa`, `bb`, `abba`, `baab`

### 4️⃣ **Lenguaje a^n b^n (Tipo 2)**
```
N = {S}
T = {a, b}
P = {
    S → a S b | ε
}
S = S
```
**Cadenas válidas:** `ε`, `ab`, `aabb`, `aaabbb`

### 5️⃣ **Identificadores (Tipo 3)**
```
N = {S, A}  
T = {a, b, 0, 1}
P = {
    S → a A | b A
    A → a A | b A | 0 A | 1 A | ε
}
S = S
```
**Cadenas válidas:** `a`, `b`, `a0`, `b1`, `ab01`

---

## 🚀 Flujo de Ejecución

### 📋 **Flujo Principal:**
```
1. Inicio de Aplicación (Main.java)
   ↓
2. Inicialización de JavaFXApp
   ↓
3. Construcción de Interfaz
   ├── Creación de MenuBar
   ├── Configuración de Paneles
   └── Aplicación de Estilos
   ↓
4. Eventos de Usuario
   ├── Cargar Gramática → GrammarPersistence.loadGrammar()
   ├── Editar Gramática → Validación en tiempo real
   ├── Parsear Cadena → ParserFactory.createParser()
   └── Generar Cadenas → StringGenerator.generateStrings()
   ↓
5. Procesamiento
   ├── Type2Parser.parse() (CFG) o Type3Parser.parse() (Regular)
   ├── DerivationTree.build() (si es exitoso)
   └── Actualización de UI
   ↓
6. Persistencia (opcional)
   └── GrammarPersistence.saveGrammar()
```

---

## ⚡ Optimizaciones Implementadas

### 🔹 **Parser Tipo 2:**
- **Límites Adaptativos:** `maxDepth = input.length * 10`, `maxSteps = input.length * 50`
- **Detección de Ciclos:** Evita bucles infinitos con estados visitados
- **Backtracking Eficiente:** Prueba producciones en orden optimal

### 🔹 **Generador de Cadenas:**
- **BFS Optimizado:** Genera cadenas por longitud creciente
- **Cache de Visitados:** Evita regenerar cadenas duplicadas
- **Límites Configurables:** Controla explosión combinatoria

### 🔹 **Interfaz de Usuario:**
- **Lazy Loading:** Cargar ejemplos solo cuando se necesitan
- **Platform.runLater():** Actualizaciones UI asíncronas
- **CSS Aggressive:** Overrides para máxima compatibilidad

---

## 🔧 Manejo de Errores

### 📋 **Tipos de Errores Manejados:**

1. **Errores de Gramática:**
   - Símbolo inicial no en N
   - Intersección N ∩ T ≠ ∅
   - Producciones con símbolos no definidos

2. **Errores de Parsing:**
   - Límites de recursión excedidos
   - Tokens no reconocidos
   - Entrada parcialmente consumida

3. **Errores de I/O:**
   - Archivos no encontrados
   - JSON malformado
   - Permisos de escritura

4. **Errores de UI:**
   - Campos vacíos o inválidos
   - Formato incorrecto de producciones
   - Símbolos multi-carácter sin espacios

---

## 🎯 Casos de Uso Típicos

### 👨‍🎓 **Para Estudiantes:**
1. **Aprender Gramáticas:** Cargar ejemplos predefinidos
2. **Experimentar:** Editar gramáticas y ver efectos inmediatos
3. **Verificar Cadenas:** Probar si una cadena pertenece al lenguaje
4. **Generar Ejemplos:** Ver cadenas válidas automáticamente

### 👨‍🏫 **Para Profesores:**
1. **Crear Ejercicios:** Diseñar gramáticas personalizadas
2. **Demostrar Conceptos:** Mostrar diferencias entre tipos
3. **Evaluar:** Verificar soluciones de estudiantes
4. **Persistir:** Guardar y compartir gramáticas

---

## 🔮 Extensibilidad

### 🔧 **Puntos de Extensión:**
- **Nuevos Tipos:** Implementar `Parser` interface para Tipo 0/1
- **Algoritmos:** Agregar parsers alternativos (LR, LALR)
- **Formatos:** Soporte para BNF, EBNF nativo
- **Visualización:** Árboles de derivación gráficos
- **Análisis:** Detección de ambigüedad, factorización

---

*Este documento describe la arquitectura y lógica completa del Sistema de Análisis de Gramáticas Formales v1.0*

**📅 Fecha:** Noviembre 2025  
**👨‍💻 Desarrollador:** JordiGD  
**🎓 Contexto:** Proyecto Universitario - Lenguajes Formales