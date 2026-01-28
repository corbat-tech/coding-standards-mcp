# Benchmark Execution: WITH MCP

## Pre-requisitos

1. Verificar que Corbat MCP está activo:
   ```bash
   claude mcp list | grep corbat
   ```

2. Si no está activo, agregarlo:
   ```bash
   claude mcp add corbat -- npx -y @anthropic/claude-code-mcp@latest
   ```

## Proceso de Ejecución

### Para cada scenario (1-10):

1. **Nueva conversación limpia** - Iniciar nueva sesión de Claude
2. **Copiar el prompt exacto** - Sin modificaciones
3. **Esperar respuesta completa** - Dejar que el LLM genere todo el código
4. **Guardar resultados** - En `results/with-mcp/scenario-XX/`

### Checklist de Métricas

Para cada scenario, registrar:

- [ ] Líneas de código (LOC): `wc -l *.java *.ts *.py *.go 2>/dev/null | tail -1`
- [ ] Número de tests: `grep -c "@Test\|it('\|test_" *.java *.ts *.py 2>/dev/null`
- [ ] Archivos generados: `ls -1 | wc -l`
- [ ] ¿Llamó a `get_context()`? (verificar en logs)
- [ ] Patrones detectados (ver sección abajo)

### Detección de Patrones

Buscar estos patrones en el código generado:

```bash
# Interface/Port pattern
grep -l "interface\|Protocol\|trait" *.java *.ts *.py *.go 2>/dev/null

# Repository pattern
grep -l "Repository\|Store" *.java *.ts *.py *.go 2>/dev/null

# Custom errors
grep -l "extends Error\|extends Exception\|class.*Error" *.java *.ts *.py *.go 2>/dev/null

# Dependency Injection
grep -l "@Inject\|@Autowired\|constructor(" *.java *.ts *.py *.go 2>/dev/null

# Hexagonal ports
grep -l "Port\|Adapter\|UseCase" *.java *.ts *.py *.go 2>/dev/null
```

---

## Prompts a Ejecutar

### Scenario 01: UserService (Basic - TypeScript)
```
Create a UserService in TypeScript.
Features: create user, get by id, list all.
Include tests.
```

### Scenario 02: REST API (Basic - Node.js)
```
Create a REST API for managing tasks.
Endpoints: POST /tasks, GET /tasks, GET /tasks/:id, DELETE /tasks/:id
Include tests.
```

### Scenario 03: React Form (Basic - React)
```
Create a contact form in React with TypeScript.
Fields: name, email, message.
Validate inputs.
Include tests.
```

### Scenario 04: Kafka Consumer (Intermediate - Java)
```
Create a Kafka consumer that processes order events.
Save processed orders to database.
Handle failures.
Include tests.
```

### Scenario 05: FastAPI Async (Intermediate - Python)
```
Create a task management API with FastAPI.
Use async SQLAlchemy.
Include CRUD operations and tests.
```

### Scenario 06: Go HTTP Handler (Intermediate - Go)
```
Create HTTP handlers for a book inventory.
Operations: CRUD, borrow, return.
Include tests.
```

### Scenario 07: Saga Pattern (Advanced - Java)
```
Create a bank transfer service.
If any step fails, rollback previous steps.
Include tests for success and failure scenarios.
```

### Scenario 08: Circuit Breaker (Advanced - TypeScript)
```
Create an HTTP client with circuit breaker pattern.
States: closed, open, half-open.
Include tests.
```

### Scenario 09: Event Sourcing (Advanced - Java)
```
Create an order management system using event sourcing.
Events: OrderCreated, ItemAdded, OrderShipped.
Include event store and projections.
Include tests.
```

### Scenario 10: State Machine (Advanced - React)
```
Create a checkout wizard component.
States: cart, shipping, payment, confirmation.
Handle transitions and validation.
Include tests.
```

---

## Notas Importantes

1. **NO modificar los prompts** - Usar exactamente como están escritos
2. **NO dar contexto adicional** - Solo el prompt
3. **Guardar respuesta completa** - Incluyendo explicaciones del LLM
4. **Registrar si MCP fue invocado** - Verificar logs de `get_context()`
