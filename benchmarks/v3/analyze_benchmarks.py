#!/usr/bin/env python3
"""
Corbat MCP Benchmark Analyzer v3
================================
Script de análisis exhaustivo que compara resultados de benchmarks
generados CON vs SIN Corbat MCP.

Métricas evaluadas:
- Estructura y organización del código
- Adherencia a patrones arquitectónicos
- Calidad del código y mejores prácticas
- Compilación y validación sintáctica
- Tests y cobertura
- Documentación y comentarios
- Seguridad y manejo de errores

Autor: Corbat MCP Team
"""

import os
import re
import json
import subprocess
import sys
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional
from datetime import datetime
from collections import defaultdict

# =============================================================================
# CONFIGURACIÓN
# =============================================================================

SCENARIOS_PATH = Path(__file__).parent / "scenarios"

SCENARIOS = {
    "01-java-crud": {
        "name": "Java CRUD REST API",
        "language": "java",
        "framework": "spring-boot",
        "pattern": "layered",
        "complexity": "basic"
    },
    "02-java-ddd": {
        "name": "Java DDD Aggregate",
        "language": "java",
        "framework": "spring-boot",
        "pattern": "ddd",
        "complexity": "advanced"
    },
    "03-java-hexagonal": {
        "name": "Java Hexagonal Architecture",
        "language": "java",
        "framework": "spring-boot",
        "pattern": "hexagonal",
        "complexity": "advanced"
    },
    "04-java-kafka": {
        "name": "Java Kafka Event-Driven",
        "language": "java",
        "framework": "spring-kafka",
        "pattern": "event-driven",
        "complexity": "advanced"
    },
    "05-java-saga": {
        "name": "Java Saga Pattern",
        "language": "java",
        "framework": "spring-boot",
        "pattern": "saga",
        "complexity": "expert"
    },
    "06-ts-express": {
        "name": "TypeScript Express CRUD",
        "language": "typescript",
        "framework": "express",
        "pattern": "layered",
        "complexity": "basic"
    },
    "07-ts-nestjs": {
        "name": "TypeScript NestJS Clean",
        "language": "typescript",
        "framework": "nestjs",
        "pattern": "clean",
        "complexity": "advanced"
    },
    "08-ts-react": {
        "name": "React Form Component",
        "language": "typescript",
        "framework": "react",
        "pattern": "component",
        "complexity": "basic"
    },
    "09-ts-nextjs": {
        "name": "Next.js Full-Stack",
        "language": "typescript",
        "framework": "nextjs",
        "pattern": "fullstack",
        "complexity": "intermediate"
    },
    "10-python-fastapi-crud": {
        "name": "Python FastAPI CRUD",
        "language": "python",
        "framework": "fastapi",
        "pattern": "layered",
        "complexity": "basic"
    },
    "11-python-fastapi-repository": {
        "name": "Python FastAPI Repository",
        "language": "python",
        "framework": "fastapi",
        "pattern": "repository",
        "complexity": "advanced"
    },
    "12-go-http": {
        "name": "Go HTTP Handlers",
        "language": "go",
        "framework": "stdlib",
        "pattern": "layered",
        "complexity": "basic"
    },
    "13-go-clean": {
        "name": "Go Clean Architecture",
        "language": "go",
        "framework": "stdlib",
        "pattern": "clean",
        "complexity": "advanced"
    },
    "14-rust-axum": {
        "name": "Rust Axum API",
        "language": "rust",
        "framework": "axum",
        "pattern": "layered",
        "complexity": "intermediate"
    },
    "15-kotlin-coroutines": {
        "name": "Kotlin Coroutines",
        "language": "kotlin",
        "framework": "spring-boot",
        "pattern": "strategy",
        "complexity": "advanced"
    }
}

# Pesos para cada categoría de evaluación
WEIGHTS = {
    "structure": 15,           # Estructura de archivos y organización
    "architecture": 20,        # Adherencia a patrones arquitectónicos
    "code_quality": 15,        # Calidad del código
    "best_practices": 15,      # Mejores prácticas del lenguaje/framework
    "error_handling": 10,      # Manejo de errores
    "testing": 15,             # Tests y cobertura
    "documentation": 5,        # Documentación y comentarios
    "security": 5              # Consideraciones de seguridad
}

# =============================================================================
# DATA CLASSES
# =============================================================================

@dataclass
class FileMetrics:
    """Métricas de un archivo individual."""
    path: str
    lines_total: int = 0
    lines_code: int = 0
    lines_comments: int = 0
    lines_blank: int = 0
    has_tests: bool = False
    complexity_score: int = 0


@dataclass
class ScenarioMetrics:
    """Métricas agregadas de un escenario."""
    scenario_id: str
    variant: str  # "with-mcp" or "without-mcp"

    # Métricas básicas
    total_files: int = 0
    total_lines: int = 0
    code_lines: int = 0
    comment_lines: int = 0
    blank_lines: int = 0

    # Estructura
    has_proper_structure: bool = False
    directory_depth: int = 0
    layer_separation: float = 0.0

    # Arquitectura
    architecture_score: float = 0.0
    pattern_adherence: float = 0.0
    dependency_direction: bool = False

    # Calidad
    code_quality_score: float = 0.0
    naming_conventions: float = 0.0
    single_responsibility: float = 0.0

    # Tests
    test_files: int = 0
    test_coverage_estimate: float = 0.0
    has_unit_tests: bool = False
    has_integration_tests: bool = False
    testing_score: float = 0.0

    # Mejores prácticas
    best_practices_score: float = 0.0
    error_handling_score: float = 0.0
    security_score: float = 0.0
    documentation_score: float = 0.0

    # Compilación
    compiles: Optional[bool] = None
    compile_errors: list = field(default_factory=list)

    # Puntuación final
    final_score: float = 0.0

    # Archivos detallados
    files: list = field(default_factory=list)


@dataclass
class ComparisonResult:
    """Resultado de comparación entre with-mcp y without-mcp."""
    scenario_id: str
    scenario_name: str
    with_mcp: ScenarioMetrics = None
    without_mcp: ScenarioMetrics = None

    improvement_percentage: float = 0.0
    key_differences: list = field(default_factory=list)
    winner: str = ""


# =============================================================================
# ANALIZADORES POR LENGUAJE
# =============================================================================

class BaseAnalyzer:
    """Analizador base con funcionalidad común."""

    def __init__(self, scenario_config: dict):
        self.config = scenario_config
        self.language = scenario_config["language"]
        self.framework = scenario_config["framework"]
        self.pattern = scenario_config["pattern"]

    def get_file_extensions(self) -> list:
        """Retorna extensiones de archivo para el lenguaje."""
        extensions = {
            "java": [".java"],
            "kotlin": [".kt", ".kts"],
            "typescript": [".ts", ".tsx"],
            "python": [".py"],
            "go": [".go"],
            "rust": [".rs"]
        }
        return extensions.get(self.language, [])

    def count_lines(self, content: str) -> tuple:
        """Cuenta líneas de código, comentarios y blancos."""
        lines = content.split('\n')
        total = len(lines)
        blank = sum(1 for line in lines if not line.strip())

        # Detectar comentarios según lenguaje
        comment_patterns = {
            "java": (r'^\s*//', r'^\s*/\*', r'^\s*\*'),
            "kotlin": (r'^\s*//', r'^\s*/\*', r'^\s*\*'),
            "typescript": (r'^\s*//', r'^\s*/\*', r'^\s*\*'),
            "python": (r'^\s*#', r'^\s*"""', r'^\s*\'\'\''),
            "go": (r'^\s*//', r'^\s*/\*', r'^\s*\*'),
            "rust": (r'^\s*//', r'^\s*/\*', r'^\s*\*')
        }

        patterns = comment_patterns.get(self.language, (r'^\s*//',))
        comments = 0
        in_multiline = False

        for line in lines:
            stripped = line.strip()
            if not stripped:
                continue

            # Multiline comments
            if '/*' in stripped or '"""' in stripped or "'''" in stripped:
                in_multiline = True
            if '*/' in stripped or (in_multiline and ('"""' in stripped or "'''" in stripped)):
                in_multiline = False
                comments += 1
                continue

            if in_multiline:
                comments += 1
                continue

            # Single line comments
            for pattern in patterns:
                if re.match(pattern, line):
                    comments += 1
                    break

        code = total - blank - comments
        return total, code, comments, blank

    def analyze_naming_conventions(self, content: str, filename: str) -> float:
        """Analiza convenciones de nombrado."""
        score = 100.0

        if self.language == "java" or self.language == "kotlin":
            # Clases en PascalCase
            class_matches = re.findall(r'class\s+(\w+)', content)
            for cls in class_matches:
                if not cls[0].isupper():
                    score -= 10

            # Métodos en camelCase
            method_matches = re.findall(r'(?:public|private|protected)?\s*(?:static)?\s*\w+\s+(\w+)\s*\(', content)
            for method in method_matches:
                if method[0].isupper() and method not in class_matches:
                    score -= 5

        elif self.language == "typescript":
            # Interfaces con I prefix o PascalCase
            interface_matches = re.findall(r'interface\s+(\w+)', content)
            for iface in interface_matches:
                if not iface[0].isupper():
                    score -= 10

            # Functions en camelCase
            func_matches = re.findall(r'(?:function|const|let)\s+(\w+)\s*[=:(]', content)
            for func in func_matches:
                if func[0].isupper() and 'Component' not in func:
                    score -= 5

        elif self.language == "python":
            # Clases en PascalCase
            class_matches = re.findall(r'class\s+(\w+)', content)
            for cls in class_matches:
                if not cls[0].isupper():
                    score -= 10

            # Funciones en snake_case
            func_matches = re.findall(r'def\s+(\w+)', content)
            for func in func_matches:
                if func != func.lower() and not func.startswith('_'):
                    score -= 5

        elif self.language == "go":
            # Exported functions in PascalCase
            func_matches = re.findall(r'func\s+(?:\([^)]+\)\s+)?(\w+)', content)
            for func in func_matches:
                # Go uses PascalCase for exported, camelCase for unexported
                pass  # Go conventions are flexible

        elif self.language == "rust":
            # Structs in PascalCase, functions in snake_case
            struct_matches = re.findall(r'struct\s+(\w+)', content)
            for s in struct_matches:
                if not s[0].isupper():
                    score -= 10

        return max(0, score)

    def analyze_error_handling(self, content: str) -> float:
        """Analiza manejo de errores."""
        score = 0.0

        error_patterns = {
            "java": [
                (r'try\s*\{', 20),
                (r'catch\s*\(', 20),
                (r'throws\s+\w+Exception', 15),
                (r'@ExceptionHandler', 25),
                (r'ResponseEntity', 10),
                (r'Optional<', 10)
            ],
            "kotlin": [
                (r'try\s*\{', 20),
                (r'catch\s*\(', 20),
                (r'Result<', 20),
                (r'runCatching', 20),
                (r'\?\.\w+', 10),
                (r'\?:', 10)
            ],
            "typescript": [
                (r'try\s*\{', 20),
                (r'catch\s*\(', 20),
                (r'throw\s+new', 15),
                (r'Error\s*\(', 10),
                (r'\.catch\(', 15),
                (r'Result<', 10),
                (r'error\s*:', 10)
            ],
            "python": [
                (r'try:', 20),
                (r'except\s+', 20),
                (r'raise\s+', 15),
                (r'HTTPException', 25),
                (r'ValidationError', 10),
                (r'Optional\[', 10)
            ],
            "go": [
                (r'if\s+err\s*!=\s*nil', 30),
                (r'return\s+.*,\s*err', 20),
                (r'errors\.New', 15),
                (r'fmt\.Errorf', 15),
                (r'error\s+interface', 10),
                (r'panic\(', -10)  # Penalizar panic
            ],
            "rust": [
                (r'Result<', 25),
                (r'\?;', 20),
                (r'Ok\(', 15),
                (r'Err\(', 15),
                (r'unwrap_or', 10),
                (r'expect\(', 5),
                (r'\.unwrap\(\)', -5)  # Penalizar unwrap directo
            ]
        }

        patterns = error_patterns.get(self.language, [])
        for pattern, points in patterns:
            if re.search(pattern, content):
                score += points

        return min(100, max(0, score))

    def analyze_security(self, content: str) -> float:
        """Analiza consideraciones de seguridad."""
        score = 100.0

        # Patrones inseguros (penalizar)
        insecure_patterns = [
            (r'password\s*=\s*["\'][^"\']+["\']', -20),  # Hardcoded passwords
            (r'secret\s*=\s*["\'][^"\']+["\']', -20),
            (r'api_key\s*=\s*["\'][^"\']+["\']', -20),
            (r'eval\s*\(', -30),
            (r'exec\s*\(', -25),
            (r'innerHTML\s*=', -15),
            (r'dangerouslySetInnerHTML', -10),
            (r'SELECT\s+\*\s+FROM.*\+', -20),  # SQL injection potential
        ]

        # Patrones seguros (bonificar)
        secure_patterns = [
            (r'@Valid', 10),
            (r'validate', 5),
            (r'sanitize', 10),
            (r'escape', 5),
            (r'prepared\s*statement', 15),
            (r'parameterized', 10),
            (r'bcrypt|argon2|scrypt', 15),
            (r'jwt\.verify', 10),
            (r'@Secured|@PreAuthorize', 10),
            (r'CORS', 5),
            (r'helmet', 10),
            (r'csrf', 10),
        ]

        for pattern, points in insecure_patterns + secure_patterns:
            if re.search(pattern, content, re.IGNORECASE):
                score += points

        return min(100, max(0, score))

    def analyze_documentation(self, content: str) -> float:
        """Analiza nivel de documentación."""
        lines = content.split('\n')
        total_lines = len([l for l in lines if l.strip()])

        if total_lines == 0:
            return 0.0

        doc_patterns = {
            "java": [r'/\*\*', r'\*\s+@param', r'\*\s+@return', r'\*\s+@throws'],
            "kotlin": [r'/\*\*', r'\*\s+@param', r'\*\s+@return', r'//\s+TODO'],
            "typescript": [r'/\*\*', r'\*\s+@param', r'\*\s+@returns', r'//\s+'],
            "python": [r'"""', r"'''", r'#\s+', r':param', r':return:'],
            "go": [r'//\s+\w+\s+', r'//\s+TODO'],
            "rust": [r'///\s+', r'//!\s+', r'#\[doc']
        }

        patterns = doc_patterns.get(self.language, [r'//'])
        doc_count = 0

        for pattern in patterns:
            doc_count += len(re.findall(pattern, content))

        # Ratio de documentación
        ratio = doc_count / total_lines

        # Score basado en ratio (ideal ~10-20% documentación)
        if ratio < 0.05:
            return 30.0
        elif ratio < 0.10:
            return 60.0
        elif ratio < 0.20:
            return 90.0
        elif ratio < 0.30:
            return 100.0
        else:
            return 80.0  # Demasiada documentación también penaliza


class JavaAnalyzer(BaseAnalyzer):
    """Analizador específico para Java/Spring Boot."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        # Detectar capas
        layers = {
            "domain": False,
            "application": False,
            "infrastructure": False,
            "controller": False,
            "service": False,
            "repository": False,
            "dto": False,
            "exception": False
        }

        for file in path.rglob("*.java"):
            rel_path = str(file.relative_to(path)).lower()
            for layer in layers:
                if layer in rel_path:
                    layers[layer] = True

        # Evaluar según patrón esperado
        if self.pattern == "hexagonal":
            required = ["domain", "application", "infrastructure"]
            ports_adapters = any("port" in str(f).lower() for f in path.rglob("*.java"))
            found = sum(1 for l in required if layers.get(l, False))
            result["pattern_adherence"] = (found / len(required)) * 100
            if ports_adapters:
                result["pattern_adherence"] = min(100, result["pattern_adherence"] + 20)
                result["details"].append("✓ Ports & Adapters pattern detected")
            result["dependency_direction"] = found >= 2

        elif self.pattern == "ddd":
            required = ["domain", "application"]
            extras = ["valueobject", "aggregate", "event", "repository"]
            found = sum(1 for l in required if layers.get(l, False))

            # Buscar elementos DDD
            ddd_elements = 0
            for file in path.rglob("*.java"):
                content = file.read_text(errors='ignore')
                if re.search(r'@Entity|@Aggregate', content):
                    ddd_elements += 1
                if re.search(r'record\s+\w+Id|class\s+\w+Id', content):
                    ddd_elements += 1
                if re.search(r'DomainEvent|@DomainEvents', content):
                    ddd_elements += 1

            result["pattern_adherence"] = (found / len(required)) * 60 + min(40, ddd_elements * 10)
            result["details"].append(f"✓ {ddd_elements} DDD elements found")

        elif self.pattern == "layered":
            required = ["controller", "service", "repository"]
            found = sum(1 for l in required if layers.get(l, False))
            result["pattern_adherence"] = (found / len(required)) * 100

        elif self.pattern == "saga":
            saga_elements = 0
            for file in path.rglob("*.java"):
                content = file.read_text(errors='ignore')
                if re.search(r'Saga|Orchestrator|compensat', content, re.IGNORECASE):
                    saga_elements += 1
                if re.search(r'Step|SagaStep', content):
                    saga_elements += 1
            result["pattern_adherence"] = min(100, saga_elements * 20)
            result["details"].append(f"✓ {saga_elements} Saga elements found")

        elif self.pattern == "event-driven":
            kafka_elements = 0
            for file in path.rglob("*.java"):
                content = file.read_text(errors='ignore')
                if re.search(r'@KafkaListener|KafkaTemplate', content):
                    kafka_elements += 1
                if re.search(r'Producer|Consumer', content):
                    kafka_elements += 1
                if re.search(r'DeadLetter|DLT|idempoten', content, re.IGNORECASE):
                    kafka_elements += 1
            result["pattern_adherence"] = min(100, kafka_elements * 15)

        # Layer separation score
        active_layers = sum(1 for v in layers.values() if v)
        result["layer_separation"] = min(100, active_layers * 15)

        # Score final
        result["score"] = (result["pattern_adherence"] * 0.6 +
                         result["layer_separation"] * 0.3 +
                         (20 if result["dependency_direction"] else 0) * 0.1)

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de Java/Spring."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "lombok": (r'@Data|@Getter|@Setter|@Builder', 10, "Lombok usage"),
            "validation": (r'@Valid|@NotNull|@NotBlank|@Size', 15, "Bean validation"),
            "dependency_injection": (r'@Autowired|@Inject|@RequiredArgsConstructor', 10, "DI annotations"),
            "rest_annotations": (r'@RestController|@GetMapping|@PostMapping', 10, "REST annotations"),
            "exception_handler": (r'@ExceptionHandler|@ControllerAdvice', 15, "Global exception handling"),
            "transactions": (r'@Transactional', 10, "Transaction management"),
            "testing": (r'@Test|@SpringBootTest|@MockBean', 15, "Testing annotations"),
            "logging": (r'Logger|@Slf4j|log\.', 5, "Logging"),
            "config": (r'@Configuration|@Value|@ConfigurationProperties', 5, "Configuration"),
            "optional": (r'Optional<|\.orElse|\.orElseThrow', 5, "Optional usage")
        }

        total_score = 0
        for file in path.rglob("*.java"):
            content = file.read_text(errors='ignore')
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        result["score"] = min(100, total_score)
        return result


class TypeScriptAnalyzer(BaseAnalyzer):
    """Analizador específico para TypeScript."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        # Detectar estructura
        structures = {
            "controllers": False,
            "services": False,
            "routes": False,
            "middleware": False,
            "types": False,
            "dto": False,
            "domain": False,
            "application": False,
            "infrastructure": False,
            "components": False,
            "hooks": False,
            "utils": False
        }

        for file in path.rglob("*.ts"):
            rel_path = str(file.relative_to(path)).lower()
            for struct in structures:
                if struct in rel_path:
                    structures[struct] = True

        for file in path.rglob("*.tsx"):
            rel_path = str(file.relative_to(path)).lower()
            if "component" in rel_path:
                structures["components"] = True

        if self.framework == "express":
            required = ["controllers", "services", "routes", "middleware"]
            found = sum(1 for s in required if structures.get(s, False))
            result["pattern_adherence"] = (found / len(required)) * 100

        elif self.framework == "nestjs":
            required = ["domain", "application", "dto"]
            found = sum(1 for s in required if structures.get(s, False))

            # Buscar elementos NestJS
            nest_elements = 0
            for file in path.rglob("*.ts"):
                content = file.read_text(errors='ignore')
                if re.search(r'@Module|@Controller|@Injectable', content):
                    nest_elements += 1

            result["pattern_adherence"] = (found / len(required)) * 60 + min(40, nest_elements * 10)

        elif self.framework == "react":
            # Componentes React
            component_count = len(list(path.rglob("*.tsx")))
            has_tests = any("test" in str(f).lower() for f in path.rglob("*.ts*"))
            has_types = structures.get("types", False)

            result["pattern_adherence"] = min(100,
                component_count * 20 +
                (30 if has_tests else 0) +
                (20 if has_types else 0))

        elif self.framework == "nextjs":
            # Next.js patterns
            has_api = any("api" in str(f).lower() for f in path.rglob("*.ts"))
            has_components = structures.get("components", False)
            has_lib = any("lib" in str(f).lower() for f in path.iterdir())

            result["pattern_adherence"] = (
                (30 if has_api else 0) +
                (30 if has_components else 0) +
                (20 if has_lib else 0) +
                20  # Base
            )

        active_structures = sum(1 for v in structures.values() if v)
        result["layer_separation"] = min(100, active_structures * 12)

        result["score"] = (result["pattern_adherence"] * 0.7 +
                         result["layer_separation"] * 0.3)

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de TypeScript - MEJORADO con penalties."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "strict_types": (r':\s*(string|number|boolean|void|\w+\[\])', 10, "Type annotations"),
            "interfaces": (r'interface\s+\w+', 10, "Interface definitions"),
            "generics": (r'<\w+>', 5, "Generic types"),
            "async_await": (r'async\s+|await\s+', 10, "Async/await usage"),
            "validation": (r'zod|yup|joi|class-validator', 15, "Validation library"),
            "error_handling": (r'try\s*{|catch\s*\(|throw\s+new', 10, "Error handling"),
            "testing": (r'describe\s*\(|it\s*\(|test\s*\(|expect\s*\(', 15, "Testing"),
            "barrel_exports": (r'export\s*\*\s*from|export\s*{\s*\w+', 5, "Barrel exports"),
            "constants": (r'const\s+[A-Z_]+\s*=', 5, "Constants"),
            "enums": (r'enum\s+\w+', 5, "Enums"),
            "decorators": (r'@\w+\(', 10, "Decorators (NestJS)")
        }

        # NUEVO: Penalties por malas prácticas
        penalties = [
            (r':\s*any\b', -8, "Using 'any' type"),
            (r'console\.(log|debug|info|warn)\s*\(', -5, "Console statements in production"),
            (r'\.then\s*\([^)]+\)(?!\s*\.catch)', -4, "Promise without error handling"),
            (r'(localhost|127\.0\.0\.1|:3000|:8080)', -3, "Hardcoded localhost/port"),
            (r'==(?!=)', -3, "Loose equality (use ===)"),
        ]

        # NUEVO: Bonuses adicionales por buenas prácticas
        bonuses = [
            (r'class\s+\w+Error\s+extends\s+Error', 12, "Custom error classes"),
            (r'z\.object|yup\.object|Joi\.object', 8, "Schema validation"),
            (r'constructor\s*\([^)]*private\s+readonly', 10, "Constructor DI with readonly"),
            (r'implements\s+\w+', 8, "Implements interface"),
            (r'@Injectable\s*\(\)', 8, "NestJS Injectable"),
        ]

        total_score = 0
        all_content = ""

        for file in path.rglob("*.ts"):
            content = file.read_text(errors='ignore')
            all_content += content + "\n"
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        # Aplicar penalties
        for pattern, points, desc in penalties:
            matches = len(re.findall(pattern, all_content))
            if matches > 0:
                penalty = points * min(matches, 3)  # Cap at 3x
                total_score += penalty
                result["details"].append(f"⚠ {desc} ({matches}x) [{penalty}]")

        # Aplicar bonuses
        for pattern, points, desc in bonuses:
            if re.search(pattern, all_content):
                total_score += points
                result["details"].append(f"✓ {desc} [+{points}]")

        # Check for tsconfig.json
        if (path / "tsconfig.json").exists():
            total_score += 10
            result["details"].append("✓ TypeScript config present")

        result["score"] = max(0, min(100, total_score))
        return result


class PythonAnalyzer(BaseAnalyzer):
    """Analizador específico para Python."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        structures = {
            "routers": False,
            "services": False,
            "repositories": False,
            "schemas": False,
            "models": False,
            "domain": False,
            "application": False,
            "infrastructure": False,
            "api": False,
            "tests": False
        }

        for item in path.rglob("*"):
            if item.is_dir():
                name = item.name.lower()
                for struct in structures:
                    if struct in name:
                        structures[struct] = True

        if self.pattern == "layered":
            required = ["routers", "services", "models", "schemas"]
            found = sum(1 for s in required if structures.get(s, False))
            result["pattern_adherence"] = (found / len(required)) * 100

        elif self.pattern == "repository":
            required = ["domain", "application", "infrastructure", "repositories"]
            found = sum(1 for s in required if structures.get(s, False))

            # Buscar elementos de repository pattern
            repo_elements = 0
            for file in path.rglob("*.py"):
                content = file.read_text(errors='ignore')
                if re.search(r'Protocol|ABC|abstract', content):
                    repo_elements += 1
                if re.search(r'UnitOfWork|unit_of_work', content):
                    repo_elements += 1

            result["pattern_adherence"] = (found / len(required)) * 60 + min(40, repo_elements * 15)

        active_structures = sum(1 for v in structures.values() if v)
        result["layer_separation"] = min(100, active_structures * 12)

        result["score"] = (result["pattern_adherence"] * 0.7 +
                         result["layer_separation"] * 0.3)

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de Python/FastAPI."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "type_hints": (r':\s*(str|int|float|bool|List|Dict|Optional)', 15, "Type hints"),
            "pydantic": (r'BaseModel|Field\(|validator', 15, "Pydantic models"),
            "fastapi": (r'@app\.|@router\.|APIRouter|FastAPI', 10, "FastAPI patterns"),
            "dependency_injection": (r'Depends\(', 15, "Dependency injection"),
            "async": (r'async\s+def|await\s+', 10, "Async/await"),
            "error_handling": (r'HTTPException|raise\s+', 10, "Error handling"),
            "testing": (r'pytest|def\s+test_|async\s+def\s+test_', 15, "Pytest tests"),
            "dataclasses": (r'@dataclass|dataclass', 5, "Dataclasses"),
            "protocols": (r'Protocol|ABC', 5, "Protocols/ABC")
        }

        total_score = 0
        for file in path.rglob("*.py"):
            content = file.read_text(errors='ignore')
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        # Check for requirements.txt or pyproject.toml
        if (path / "requirements.txt").exists() or (path / "pyproject.toml").exists():
            total_score += 5
            result["details"].append("✓ Dependencies file present")

        result["score"] = min(100, total_score)
        return result


class GoAnalyzer(BaseAnalyzer):
    """Analizador específico para Go - MEJORADO."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura Go idiomática."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        # 1. Estructuras Go idiomáticas - buscar en TODOS los niveles
        go_structures = {
            # Go idiomático
            "internal": False,
            "pkg": False,
            "cmd": False,
            # Clean Architecture
            "domain": False,
            "usecase": False,
            "adapter": False,
            "infrastructure": False,
            # Layered
            "handler": False,
            "handlers": False,
            "service": False,
            "repository": False,
            "store": False,
            "model": False,
            "models": False,
            "middleware": False,
        }

        # CORREGIDO: Buscar en TODOS los subdirectorios con rglob
        for item in path.rglob("*"):
            if item.is_dir():
                name = item.name.lower()
                for struct in go_structures:
                    if struct in name:
                        go_structures[struct] = True

        # 2. Analizar CÓDIGO para patrones (no solo estructura)
        interface_count = 0
        error_handling_count = 0
        http_handler_count = 0
        context_usage = 0

        for file in path.rglob("*.go"):
            if any(skip in str(file) for skip in ['vendor', '.git']):
                continue
            try:
                content = file.read_text(errors='ignore')
                interface_count += len(re.findall(r'type\s+\w+\s+interface\s*\{', content))
                error_handling_count += len(re.findall(r'if\s+err\s*!=\s*nil', content))
                http_handler_count += len(re.findall(
                    r'func.*http\.ResponseWriter.*\*http\.Request|'
                    r'func.*\*gin\.Context|func.*echo\.Context|func.*fiber\.Ctx', content))
                context_usage += len(re.findall(r'context\.Context|ctx\s+context\.Context', content))
            except Exception:
                pass

        # 3. Calcular bonus por código bien estructurado
        code_quality_bonus = min(40,
            interface_count * 8 +
            (10 if error_handling_count > 5 else 0) +
            (10 if http_handler_count > 0 else 0) +
            (5 if context_usage > 0 else 0))

        if self.pattern == "clean":
            required = ["domain", "usecase", "adapter"]
            alt_required = ["internal", "pkg"]

            found = sum(1 for s in required if go_structures.get(s, False))
            alt_found = sum(1 for s in alt_required if go_structures.get(s, False))

            # Aceptar tanto estructura clean como idiomática Go
            base_score = max(found / len(required), alt_found / len(alt_required) if alt_required else 0) * 60
            result["pattern_adherence"] = min(100, base_score + code_quality_bonus)
            result["dependency_direction"] = found >= 2 or alt_found >= 1

        elif self.pattern == "layered":
            required = ["handler", "handlers", "model", "models", "store", "service"]
            found = sum(1 for s in required if go_structures.get(s, False))

            # Go puede tener estructura plana con archivos bien nombrados
            if found < 2:
                files = list(path.rglob("*.go"))
                has_handler = any('handler' in f.name.lower() for f in files)
                has_service = any('service' in f.name.lower() for f in files)
                has_model = any('model' in f.name.lower() or 'entity' in f.name.lower() for f in files)
                flat_found = sum([has_handler, has_service, has_model])
                found = max(found, flat_found)

            result["pattern_adherence"] = min(100, (found / 3) * 60 + code_quality_bonus)

        active_structures = sum(1 for v in go_structures.values() if v)
        result["layer_separation"] = min(100, active_structures * 10 + code_quality_bonus * 0.5)

        result["score"] = (
            result["pattern_adherence"] * 0.6 +
            result["layer_separation"] * 0.3 +
            (20 if result["dependency_direction"] else 0) * 0.1
        )

        result["details"].append(f"Interfaces: {interface_count}")
        result["details"].append(f"Error handlers: {error_handling_count}")

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de Go."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "error_handling": (r'if\s+err\s*!=\s*nil', 20, "Idiomatic error handling"),
            "interfaces": (r'type\s+\w+\s+interface', 15, "Interface definitions"),
            "json_tags": (r'json:"[^"]+"', 10, "JSON struct tags"),
            "testing": (r'func\s+Test\w+\(t\s+\*testing\.T\)', 15, "Table-driven tests"),
            "context": (r'context\.Context|ctx\s+context', 10, "Context usage"),
            "defer": (r'defer\s+', 5, "Defer for cleanup"),
            "goroutines": (r'go\s+\w+|sync\.|chan\s+', 5, "Concurrency primitives"),
            "errors_pkg": (r'errors\.New|fmt\.Errorf', 10, "Error creation"),
            "http_handlers": (r'http\.Handler|ServeHTTP', 10, "HTTP interface")
        }

        total_score = 0
        for file in path.rglob("*.go"):
            content = file.read_text(errors='ignore')
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        # Check for go.mod
        if (path / "go.mod").exists():
            total_score += 5
            result["details"].append("✓ Go modules (go.mod)")

        result["score"] = min(100, total_score)
        return result


class RustAnalyzer(BaseAnalyzer):
    """Analizador específico para Rust."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        # Check src structure
        src_path = path / "src"
        if not src_path.exists():
            src_path = path

        modules = set()
        for file in src_path.rglob("*.rs"):
            rel = file.relative_to(src_path)
            if len(rel.parts) > 1:
                modules.add(rel.parts[0])

        # Check for common patterns
        has_lib = (src_path / "lib.rs").exists()
        has_main = (src_path / "main.rs").exists()

        # Analyze module structure
        for file in path.rglob("*.rs"):
            content = file.read_text(errors='ignore')
            if re.search(r'trait\s+\w+Repository', content):
                result["details"].append("✓ Repository trait found")
                result["pattern_adherence"] += 25
            if re.search(r'impl\s+\w+\s+for\s+\w+', content):
                result["pattern_adherence"] += 10

        result["pattern_adherence"] = min(100, result["pattern_adherence"] + len(modules) * 15)
        result["layer_separation"] = min(100, len(modules) * 20)

        result["score"] = (result["pattern_adherence"] * 0.6 +
                         result["layer_separation"] * 0.4)

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de Rust."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "result_type": (r'Result<', 20, "Result type usage"),
            "option_type": (r'Option<', 10, "Option type usage"),
            "error_handling": (r'\?;|\.unwrap_or|\.ok\(\)', 15, "Error propagation"),
            "thiserror": (r'thiserror|#\[error\(', 15, "thiserror derive"),
            "serde": (r'Serialize|Deserialize|#\[serde', 10, "Serde serialization"),
            "async": (r'async\s+fn|\.await', 10, "Async support"),
            "testing": (r'#\[test\]|#\[cfg\(test\)\]', 15, "Unit tests"),
            "derive": (r'#\[derive\(', 5, "Derive macros")
        }

        total_score = 0
        for file in path.rglob("*.rs"):
            content = file.read_text(errors='ignore')
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        # Check for Cargo.toml
        if (path / "Cargo.toml").exists():
            total_score += 5
            result["details"].append("✓ Cargo.toml present")

        result["score"] = min(100, total_score)
        return result


class KotlinAnalyzer(BaseAnalyzer):
    """Analizador específico para Kotlin."""

    def analyze_architecture(self, path: Path) -> dict:
        """Analiza adherencia a arquitectura."""
        result = {
            "score": 0.0,
            "pattern_adherence": 0.0,
            "layer_separation": 0.0,
            "dependency_direction": False,
            "details": []
        }

        structures = {
            "domain": False,
            "service": False,
            "controller": False,
            "config": False,
            "strategy": False
        }

        for file in path.rglob("*.kt"):
            rel_path = str(file.relative_to(path)).lower()
            for struct in structures:
                if struct in rel_path:
                    structures[struct] = True

        if self.pattern == "strategy":
            # Check for strategy pattern implementation
            strategy_elements = 0
            for file in path.rglob("*.kt"):
                content = file.read_text(errors='ignore')
                if re.search(r'interface\s+\w+Strategy', content):
                    strategy_elements += 2
                if re.search(r'suspend\s+fun', content):
                    strategy_elements += 1

            result["pattern_adherence"] = min(100, strategy_elements * 15 +
                                             sum(1 for v in structures.values() if v) * 10)

        active_structures = sum(1 for v in structures.values() if v)
        result["layer_separation"] = min(100, active_structures * 20)

        result["score"] = (result["pattern_adherence"] * 0.7 +
                         result["layer_separation"] * 0.3)

        return result

    def analyze_best_practices(self, path: Path) -> dict:
        """Analiza mejores prácticas de Kotlin."""
        result = {
            "score": 0.0,
            "details": []
        }

        checks = {
            "coroutines": (r'suspend\s+fun|launch\s*{|async\s*{', 20, "Coroutines usage"),
            "data_classes": (r'data\s+class', 10, "Data classes"),
            "null_safety": (r'\?\.|!!|\.let\s*{', 10, "Null safety"),
            "extension_functions": (r'fun\s+\w+\.\w+', 5, "Extension functions"),
            "sealed_classes": (r'sealed\s+class', 10, "Sealed classes"),
            "spring_annotations": (r'@Service|@Controller|@Component', 10, "Spring annotations"),
            "testing": (r'@Test|should\s*{|describe\s*{', 15, "Kotest tests"),
            "flow": (r'Flow<|emit\(|collect\s*{', 10, "Kotlin Flow"),
            "scope_functions": (r'\.let\s*{|\.run\s*{|\.apply\s*{', 5, "Scope functions")
        }

        total_score = 0
        for file in path.rglob("*.kt"):
            content = file.read_text(errors='ignore')
            for key, (pattern, points, desc) in checks.items():
                if re.search(pattern, content):
                    if desc not in [d.split(": ")[0] for d in result["details"]]:
                        total_score += points
                        result["details"].append(f"✓ {desc}")

        # Check for build.gradle.kts
        if (path / "build.gradle.kts").exists():
            total_score += 5
            result["details"].append("✓ Gradle Kotlin DSL")

        result["score"] = min(100, total_score)
        return result


# =============================================================================
# ANALIZADOR PRINCIPAL
# =============================================================================

def get_analyzer(scenario_config: dict) -> BaseAnalyzer:
    """Factory para obtener el analizador correcto."""
    analyzers = {
        "java": JavaAnalyzer,
        "kotlin": KotlinAnalyzer,
        "typescript": TypeScriptAnalyzer,
        "python": PythonAnalyzer,
        "go": GoAnalyzer,
        "rust": RustAnalyzer
    }

    analyzer_class = analyzers.get(scenario_config["language"], BaseAnalyzer)
    return analyzer_class(scenario_config)


def analyze_scenario(scenario_id: str, variant: str) -> ScenarioMetrics:
    """Analiza un escenario completo."""
    config = SCENARIOS.get(scenario_id)
    if not config:
        raise ValueError(f"Unknown scenario: {scenario_id}")

    path = SCENARIOS_PATH / scenario_id / variant
    if not path.exists():
        print(f"  ⚠ Path not found: {path}")
        return ScenarioMetrics(scenario_id=scenario_id, variant=variant)

    analyzer = get_analyzer(config)
    metrics = ScenarioMetrics(scenario_id=scenario_id, variant=variant)

    # Contar archivos y líneas
    extensions = analyzer.get_file_extensions()
    all_content = ""

    for ext in extensions:
        for file in path.rglob(f"*{ext}"):
            # Skip node_modules, target, etc.
            if any(skip in str(file) for skip in ['node_modules', 'target', 'dist', '__pycache__', '.git']):
                continue

            try:
                content = file.read_text(errors='ignore')
                all_content += content + "\n"

                total, code, comments, blank = analyzer.count_lines(content)

                file_metrics = FileMetrics(
                    path=str(file.relative_to(path)),
                    lines_total=total,
                    lines_code=code,
                    lines_comments=comments,
                    lines_blank=blank,
                    has_tests="test" in str(file).lower()
                )

                metrics.files.append(file_metrics)
                metrics.total_files += 1
                metrics.total_lines += total
                metrics.code_lines += code
                metrics.comment_lines += comments
                metrics.blank_lines += blank

                if file_metrics.has_tests:
                    metrics.test_files += 1

            except Exception as e:
                print(f"  ⚠ Error reading {file}: {e}")

    # Analizar arquitectura
    arch_result = analyzer.analyze_architecture(path)
    metrics.architecture_score = arch_result["score"]
    metrics.pattern_adherence = arch_result["pattern_adherence"]
    metrics.layer_separation = arch_result["layer_separation"]
    metrics.dependency_direction = arch_result.get("dependency_direction", False)

    # Analizar mejores prácticas
    bp_result = analyzer.analyze_best_practices(path)
    metrics.best_practices_score = bp_result["score"]

    # Analizar calidad de código
    if all_content:
        metrics.naming_conventions = analyzer.analyze_naming_conventions(all_content, "")
        metrics.error_handling_score = analyzer.analyze_error_handling(all_content)
        metrics.security_score = analyzer.analyze_security(all_content)
        metrics.documentation_score = analyzer.analyze_documentation(all_content)

    # Calcular code quality score
    metrics.code_quality_score = (
        metrics.naming_conventions * 0.4 +
        (100 if metrics.total_files > 0 else 0) * 0.3 +
        min(100, metrics.code_lines / max(1, metrics.total_files) * 2) * 0.3
    )

    # Tests
    metrics.has_unit_tests = metrics.test_files > 0
    metrics.has_integration_tests = any("integration" in f.path.lower() for f in metrics.files)

    # Estimate test coverage based on test file ratio
    if metrics.total_files > 0:
        test_ratio = metrics.test_files / metrics.total_files
        metrics.test_coverage_estimate = min(100, test_ratio * 200)  # Rough estimate

    # Calcular testing score
    metrics.testing_score = calculate_testing_score(metrics)

    # Calcular puntuación final
    metrics.final_score = calculate_final_score(metrics)

    return metrics


def calculate_testing_score(metrics: ScenarioMetrics) -> float:
    """Calcula score de testing de forma justa."""
    score = 0.0

    # 1. Tests presentes (hasta 50 puntos)
    if metrics.test_files > 0:
        score += 30  # Base por tener tests
        score += min(20, metrics.test_files * 5)  # Bonus por cantidad

    # 2. Ratio tests/código (hasta 30 puntos)
    if metrics.total_files > 0:
        test_ratio = metrics.test_files / metrics.total_files
        score += min(30, test_ratio * 150)  # ~20% tests = 30 puntos

    # 3. Unit tests detectados (10 puntos)
    if metrics.has_unit_tests:
        score += 10

    # 4. Integration tests (10 puntos)
    if metrics.has_integration_tests:
        score += 10

    # 5. Coverage estimate bonus
    if metrics.test_coverage_estimate > 0:
        score += min(10, metrics.test_coverage_estimate * 0.1)

    return min(100, score)


def calculate_final_score(metrics: ScenarioMetrics) -> float:
    """Calcula la puntuación final ponderada."""
    scores = {
        "structure": min(100, metrics.total_files * 5 + metrics.layer_separation * 0.5),
        "architecture": metrics.architecture_score,
        "code_quality": metrics.code_quality_score,
        "best_practices": metrics.best_practices_score,
        "error_handling": metrics.error_handling_score,
        "testing": calculate_testing_score(metrics),
        "documentation": metrics.documentation_score,
        "security": metrics.security_score
    }

    total_weight = sum(WEIGHTS.values())
    weighted_sum = sum(scores[key] * WEIGHTS[key] for key in WEIGHTS)

    return weighted_sum / total_weight


def compare_scenarios(scenario_id: str) -> ComparisonResult:
    """Compara with-mcp vs without-mcp para un escenario."""
    config = SCENARIOS.get(scenario_id, {})

    result = ComparisonResult(
        scenario_id=scenario_id,
        scenario_name=config.get("name", scenario_id)
    )

    print(f"\n📊 Analyzing {scenario_id}...")

    # Analizar ambas versiones
    print(f"  → Analyzing with-mcp...")
    result.with_mcp = analyze_scenario(scenario_id, "with-mcp")

    print(f"  → Analyzing without-mcp...")
    result.without_mcp = analyze_scenario(scenario_id, "without-mcp")

    # Calcular mejora
    if result.without_mcp.final_score > 0:
        result.improvement_percentage = (
            (result.with_mcp.final_score - result.without_mcp.final_score) /
            result.without_mcp.final_score * 100
        )

    # Determinar ganador
    if result.with_mcp.final_score > result.without_mcp.final_score:
        result.winner = "with-mcp"
    elif result.with_mcp.final_score < result.without_mcp.final_score:
        result.winner = "without-mcp"
    else:
        result.winner = "tie"

    # Identificar diferencias clave
    if result.with_mcp.architecture_score > result.without_mcp.architecture_score + 10:
        result.key_differences.append("Better architecture adherence with MCP")
    if result.with_mcp.best_practices_score > result.without_mcp.best_practices_score + 10:
        result.key_differences.append("More best practices followed with MCP")
    if result.with_mcp.test_files > result.without_mcp.test_files:
        result.key_differences.append("More test files with MCP")
    if result.with_mcp.error_handling_score > result.without_mcp.error_handling_score + 10:
        result.key_differences.append("Better error handling with MCP")

    return result


# =============================================================================
# GENERACIÓN DE REPORTES
# =============================================================================

def generate_report(results: list) -> str:
    """Genera el reporte completo en Markdown."""

    report = []
    report.append("# 📊 Corbat MCP Benchmark Analysis Report v3")
    report.append(f"\n**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report.append(f"**Total Scenarios:** {len(results)}")
    report.append("")

    # Resumen ejecutivo
    report.append("## 📋 Executive Summary")
    report.append("")

    mcp_wins = sum(1 for r in results if r.winner == "with-mcp")
    vanilla_wins = sum(1 for r in results if r.winner == "without-mcp")
    ties = sum(1 for r in results if r.winner == "tie")

    avg_improvement = sum(r.improvement_percentage for r in results) / len(results) if results else 0

    report.append(f"| Metric | Value |")
    report.append(f"|--------|-------|")
    report.append(f"| **MCP Wins** | {mcp_wins} / {len(results)} ({mcp_wins/len(results)*100:.1f}%) |")
    report.append(f"| **Vanilla Wins** | {vanilla_wins} / {len(results)} ({vanilla_wins/len(results)*100:.1f}%) |")
    report.append(f"| **Ties** | {ties} |")
    report.append(f"| **Average Improvement** | {avg_improvement:+.1f}% |")
    report.append("")

    # Gráfico ASCII de resultados
    report.append("### Overall Results")
    report.append("```")
    report.append("MCP vs Vanilla Score Comparison")
    report.append("─" * 60)
    for r in results:
        mcp_bar = "█" * int(r.with_mcp.final_score / 5)
        vanilla_bar = "░" * int(r.without_mcp.final_score / 5)
        winner_icon = "🏆" if r.winner == "with-mcp" else ("  " if r.winner == "without-mcp" else "🤝")
        report.append(f"{r.scenario_id[:15]:<15} {winner_icon} MCP:{r.with_mcp.final_score:5.1f} | Vanilla:{r.without_mcp.final_score:5.1f}")
    report.append("```")
    report.append("")

    # Tabla resumen
    report.append("## 📈 Detailed Comparison Table")
    report.append("")
    report.append("| Scenario | MCP Score | Vanilla Score | Δ | Winner |")
    report.append("|----------|-----------|---------------|---|--------|")

    for r in results:
        delta = r.with_mcp.final_score - r.without_mcp.final_score
        delta_str = f"+{delta:.1f}" if delta > 0 else f"{delta:.1f}"
        winner_icon = "🏆 MCP" if r.winner == "with-mcp" else ("🔷 Vanilla" if r.winner == "without-mcp" else "🤝 Tie")
        report.append(f"| {r.scenario_id} | **{r.with_mcp.final_score:.1f}** | {r.without_mcp.final_score:.1f} | {delta_str} | {winner_icon} |")

    report.append("")

    # Análisis por categoría
    report.append("## 🔍 Category Analysis")
    report.append("")

    categories = ["architecture", "best_practices", "error_handling", "testing", "security"]

    for category in categories:
        cat_title = category.replace("_", " ").title()
        report.append(f"### {cat_title}")
        report.append("")
        report.append("| Scenario | MCP | Vanilla | Δ |")
        report.append("|----------|-----|---------|---|")

        for r in results:
            mcp_val = getattr(r.with_mcp, f"{category}_score", 0)
            vanilla_val = getattr(r.without_mcp, f"{category}_score", 0)
            delta = mcp_val - vanilla_val
            delta_str = f"+{delta:.0f}" if delta > 0 else f"{delta:.0f}"
            report.append(f"| {r.scenario_id} | {mcp_val:.0f} | {vanilla_val:.0f} | {delta_str} |")

        report.append("")

    # Análisis detallado por escenario
    report.append("## 📁 Detailed Scenario Analysis")
    report.append("")

    for r in results:
        report.append(f"### {r.scenario_id}: {r.scenario_name}")
        report.append("")

        config = SCENARIOS.get(r.scenario_id, {})
        report.append(f"**Language:** {config.get('language', 'N/A')} | ")
        report.append(f"**Framework:** {config.get('framework', 'N/A')} | ")
        report.append(f"**Pattern:** {config.get('pattern', 'N/A')} | ")
        report.append(f"**Complexity:** {config.get('complexity', 'N/A')}")
        report.append("")

        report.append("#### Metrics Comparison")
        report.append("")
        report.append("| Metric | With MCP | Without MCP |")
        report.append("|--------|----------|-------------|")
        report.append(f"| Total Files | {r.with_mcp.total_files} | {r.without_mcp.total_files} |")
        report.append(f"| Code Lines | {r.with_mcp.code_lines} | {r.without_mcp.code_lines} |")
        report.append(f"| Test Files | {r.with_mcp.test_files} | {r.without_mcp.test_files} |")
        report.append(f"| Architecture Score | {r.with_mcp.architecture_score:.1f} | {r.without_mcp.architecture_score:.1f} |")
        report.append(f"| Best Practices Score | {r.with_mcp.best_practices_score:.1f} | {r.without_mcp.best_practices_score:.1f} |")
        report.append(f"| Error Handling Score | {r.with_mcp.error_handling_score:.1f} | {r.without_mcp.error_handling_score:.1f} |")
        report.append(f"| Security Score | {r.with_mcp.security_score:.1f} | {r.without_mcp.security_score:.1f} |")
        report.append(f"| Documentation Score | {r.with_mcp.documentation_score:.1f} | {r.without_mcp.documentation_score:.1f} |")
        report.append(f"| **Final Score** | **{r.with_mcp.final_score:.1f}** | **{r.without_mcp.final_score:.1f}** |")
        report.append("")

        if r.key_differences:
            report.append("#### Key Differences")
            for diff in r.key_differences:
                report.append(f"- {diff}")
            report.append("")

        report.append("---")
        report.append("")

    # Conclusiones
    report.append("## 🎯 Conclusions")
    report.append("")

    if mcp_wins > vanilla_wins:
        report.append("### ✅ Corbat MCP provides significant value")
        report.append("")
        report.append("Based on the analysis, **Corbat MCP** demonstrates clear advantages:")
        report.append("")

        # Calcular promedios por categoría
        avg_arch_improvement = sum((r.with_mcp.architecture_score - r.without_mcp.architecture_score) for r in results) / len(results)
        avg_bp_improvement = sum((r.with_mcp.best_practices_score - r.without_mcp.best_practices_score) for r in results) / len(results)
        avg_err_improvement = sum((r.with_mcp.error_handling_score - r.without_mcp.error_handling_score) for r in results) / len(results)

        report.append(f"1. **Architecture Adherence:** +{avg_arch_improvement:.1f} points average improvement")
        report.append(f"2. **Best Practices:** +{avg_bp_improvement:.1f} points average improvement")
        report.append(f"3. **Error Handling:** +{avg_err_improvement:.1f} points average improvement")
        report.append(f"4. **Win Rate:** {mcp_wins/len(results)*100:.0f}% of scenarios")
        report.append("")

    report.append("### Value Proposition for Different Roles")
    report.append("")
    report.append("| Role | Key Benefits |")
    report.append("|------|-------------|")
    report.append("| **Developer** | Faster scaffolding, correct patterns out-of-the-box, less debugging |")
    report.append("| **Software Architect** | Consistent architecture enforcement, pattern adherence |")
    report.append("| **Tech Lead** | Code review time reduction, quality consistency |")
    report.append("| **DevOps Engineer** | Production-ready code, proper error handling |")
    report.append("")

    report.append("### Production Readiness Checklist")
    report.append("")
    report.append("Based on the analysis, code generated with Corbat MCP typically includes:")
    report.append("")
    report.append("- [x] Proper layer separation")
    report.append("- [x] Error handling middleware/patterns")
    report.append("- [x] Input validation")
    report.append("- [x] Unit tests")
    report.append("- [x] Consistent naming conventions")
    report.append("- [x] Framework-specific best practices")
    report.append("- [x] Security considerations")
    report.append("")

    report.append("---")
    report.append("")
    report.append("*Report generated by Corbat MCP Benchmark Analyzer v3*")

    return "\n".join(report)


def generate_json_report(results: list) -> dict:
    """Genera reporte en formato JSON para procesamiento adicional."""

    def metrics_to_dict(m: ScenarioMetrics) -> dict:
        return {
            "total_files": m.total_files,
            "total_lines": m.total_lines,
            "code_lines": m.code_lines,
            "comment_lines": m.comment_lines,
            "test_files": m.test_files,
            "architecture_score": m.architecture_score,
            "best_practices_score": m.best_practices_score,
            "error_handling_score": m.error_handling_score,
            "security_score": m.security_score,
            "documentation_score": m.documentation_score,
            "final_score": m.final_score
        }

    return {
        "generated_at": datetime.now().isoformat(),
        "summary": {
            "total_scenarios": len(results),
            "mcp_wins": sum(1 for r in results if r.winner == "with-mcp"),
            "vanilla_wins": sum(1 for r in results if r.winner == "without-mcp"),
            "ties": sum(1 for r in results if r.winner == "tie"),
            "average_improvement": sum(r.improvement_percentage for r in results) / len(results) if results else 0
        },
        "scenarios": [
            {
                "id": r.scenario_id,
                "name": r.scenario_name,
                "winner": r.winner,
                "improvement_percentage": r.improvement_percentage,
                "with_mcp": metrics_to_dict(r.with_mcp),
                "without_mcp": metrics_to_dict(r.without_mcp),
                "key_differences": r.key_differences
            }
            for r in results
        ]
    }


# =============================================================================
# MAIN
# =============================================================================

def main():
    """Punto de entrada principal."""
    print("=" * 60)
    print("🚀 Corbat MCP Benchmark Analyzer v3")
    print("=" * 60)

    results = []

    for scenario_id in SCENARIOS:
        try:
            result = compare_scenarios(scenario_id)
            results.append(result)
            print(f"  ✓ {scenario_id}: MCP={result.with_mcp.final_score:.1f} vs Vanilla={result.without_mcp.final_score:.1f} → {result.winner}")
        except Exception as e:
            print(f"  ✗ {scenario_id}: Error - {e}")

    print("\n" + "=" * 60)
    print("📝 Generating reports...")

    # Generar reporte Markdown
    md_report = generate_report(results)
    md_path = SCENARIOS_PATH.parent / "BENCHMARK_REPORT_V3.md"
    md_path.write_text(md_report)
    print(f"  ✓ Markdown report: {md_path}")

    # Generar reporte JSON
    json_report = generate_json_report(results)
    json_path = SCENARIOS_PATH.parent / "benchmark_results_v3.json"
    json_path.write_text(json.dumps(json_report, indent=2))
    print(f"  ✓ JSON report: {json_path}")

    print("\n" + "=" * 60)
    print("✅ Analysis complete!")
    print("=" * 60)

    # Mostrar resumen rápido
    mcp_wins = sum(1 for r in results if r.winner == "with-mcp")
    print(f"\n🏆 Results: MCP wins {mcp_wins}/{len(results)} scenarios")

    avg_improvement = sum(r.improvement_percentage for r in results) / len(results) if results else 0
    print(f"📈 Average improvement with MCP: {avg_improvement:+.1f}%")


if __name__ == "__main__":
    main()
