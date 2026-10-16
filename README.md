# PediuPatrão — Atividade M1

Sistema de gestão de uma pizzaria (clientes, produtos, pedidos) evoluído com **controle de acesso por perfil**, **fluxo controlado de pedidos** e **auditoria persistente**. Atividade prática individual de Engenharia de Software.

**Tecnologias:** Java 21, Spring Boot 3.4.4, Spring Security 6, Thymeleaf, MongoDB.

## Como executar

Pré-requisitos (tudo roda localmente, sem Docker):

- JDK 21
- MongoDB Community em `localhost:27017` (instalado como serviço do Windows, sem autenticação)

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse <http://localhost:8080>. O Maven Wrapper baixa o Maven automaticamente na primeira execução.

Para rodar os testes (usam o banco separado `pizzaria_test`, limpo a cada teste):

```powershell
.\mvnw.cmd test
```

### Usuários de demonstração

Criados automaticamente na primeira execução (`SemeadorDeContas`). Servem apenas para uso local; as senhas ficam em `application.properties` (`pediupatrao.demonstracao.*`).

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `luffy` | ADMIN |
| `gerente01` | `zoro` | GERENTE |
| `atendente01` | `chopper` | ATENDENTE |

O sistema cria também a grade padrão de horários de funcionamento (todos os dias, 18:00 às 23:00). Produtos, clientes, funcionários e entregadores devem ser cadastrados pelas telas (produtos, funcionários e entregadores com o `gerente01`; clientes com o `atendente01`).

## Perfis e permissões

| Capacidade | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Criar, editar e remover usuários; atribuir perfis | ✅ | ❌ | ❌ |
| Consultar pedidos | ✅ | ✅ | ✅ |
| Registrar pedido e avançar o status | ❌ | ✅ | ✅ |
| Aplicar desconto | ❌ | ✅ | ❌ |
| Cancelar pedido | ❌ | ✅ | ❌ |
| Cadastrar e alterar clientes | ❌ | ❌ | ✅ |
| Consultar clientes | ❌ | ✅ | ✅ |
| Inativar cliente | ❌ | ✅ | ❌ |
| Consultar produtos | ✅ | ✅ | ✅ |
| Criar, editar e remover produtos | ❌ | ✅ | ❌ |
| Consultar funcionários, entregadores e horários | ✅ | ✅ | ✅ |
| Criar, editar e inativar funcionários e entregadores; alterar horários | ❌ | ✅ | ❌ |
| Consultar auditoria | ✅ | ✅ | ❌ |

A autorização é feita **no backend**: regras por rota em `SecurityConfig` (qualquer rota não listada é negada) e `@PreAuthorize` nos serviços como segunda camada. Esconder botões nas telas é só conveniência e não substitui essas regras.

## Requisitos implementados

### RF01 — Usuários e permissões
- Três perfis reais (`Perfil`), gravados no banco e convertidos em autoridades pelo `UsuarioDetailsService`. Usuário sem perfil válido não acessa nada.
- API de usuários restrita ao ADMIN; a resposta nunca inclui a senha (hash).
- Proteção contra autoelevação: ninguém altera o próprio perfil nem exclui a si mesmo, o último ADMIN não pode ser rebaixado ou removido e o perfil é validado contra a lista permitida.
- CSRF habilitado. Sem login a API responde 401; sem permissão, 403 (JSON); as telas redirecionam ao login.

### RF02 — Pedido com itens e valores
- `Pedido` guarda cliente, itens (produto, preço e nome copiados no momento do pedido, quantidade, subtotal), tipo de saída, subtotal, desconto, total, responsável e data/hora de entrada.
- O total é **calculado no servidor** (`CalculadoraDePedido`, `BigDecimal`, 2 casas, arredondamento `HALF_UP`) com o preço do cadastro. A requisição só aceita cliente, tipo de saída e itens: total, desconto, status e responsável enviados pelo cliente HTTP são ignorados.

### RF03 — Fluxo de status
```
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO   (saída por ENTREGA)
                                  └→ RETIRADO ──────────→ FINALIZADO   (saída por RETIRADA)
CANCELADO: exceção, só do GERENTE, antes da saída
```
- `POST /api/pedidos/{id}/avancar` leva ao próximo passo válido. O corpo pode trazer um `destino` opcional; se ele não for o passo seguinte, a resposta é 409 (ex.: finalizar um pedido em preparação).
- Ao sair para entrega ou ser retirado, grava data/hora e usuário da saída. Na saída por **entrega** o entregador responsável é obrigatório (deve estar ativo) e fica gravado no pedido e na auditoria.
- Pedidos `FINALIZADO` ou `CANCELADO` não aceitam mais alteração.

### RF04 — Cancelamento e desconto (somente GERENTE)
- **Cancelamento** (`POST /api/pedidos/{id}/cancelar`): só até `PRONTO`; exige um motivo escolhido numa lista pré-definida (cliente desistiu, erro no pedido, item indisponível, demora na preparação, pagamento recusado ou outro; "outro" exige observação de ao menos 10 caracteres). Itens e valores permanecem no banco.
- **Desconto** (`POST /api/pedidos/{id}/desconto`): percentual de 0% a 20% sobre o subtotal, apenas antes da saída; reaplicar substitui o anterior e 0 remove.
- ADMIN e ATENDENTE recebem 403 nas duas operações.

### RF05 — Auditoria
- Coleção `auditoria` no MongoDB, **somente inserção**: o registro é imutável (`record`), o repositório só expõe `save` e não há endpoint de alteração ou exclusão.
- Cada registro guarda usuário, perfil, data/hora, operação, entidade e id, campos alterados (valor anterior e novo) e justificativa quando exigida.
- Operações auditadas: criação de pedido, mudança de status, desconto, cancelamento, criação e alteração de cliente (telefone e endereço), inativação e expurgo de cliente.
- Consulta em `/auditoria` (tela) e `GET /api/auditoria` (filtros por entidade, identificador, usuário e período, com paginação), restrita a ADMIN e GERENTE.
- Senhas, hashes e tokens nunca são gravados. Na alteração de clientes são auditados nome, telefone e endereço, sempre com os valores anterior e novo; como esses dados pessoais são o objeto da auditoria, o acesso é restrito a ADMIN e GERENTE e nenhum outro dado do cliente é copiado.

Exemplo de registro (alteração de cliente):

| Campo | Valor |
|---|---|
| Usuário | atendente01 (ATENDENTE) |
| Operação | ALTERACAO_CLIENTE |
| Campo alterado | logradouro |
| Valor anterior | Rua das Flores |
| Novo valor | Av. Brasil |
| Data e hora | 23/09/2026 — 14:35 |

## Principais alterações realizadas

Comparação com a situação inicial descrita no enunciado:

| Componente | Situação inicial | O que foi feito |
|---|---|---|
| Usuários | Perfis ADMIN e USER; a autenticação atribuía sempre a role USER | Três perfis reais (ADMIN, GERENTE, ATENDENTE) gravados no banco e usados na autenticação; atribuição de perfil restrita ao ADMIN, com proteção contra autoelevação; a senha não é mais devolvida pela API |
| Pedidos | Criação, listagem e troca de status livre | Itens, quantidades e preços copiados do cadastro; total calculado no servidor; responsável e data/hora de entrada; fluxo de status com transições validadas |
| Clientes | Cadastro, consulta, alteração e exclusão | Cadastro e alteração pelo ATENDENTE, consulta pelo GERENTE; a exclusão deu lugar à inativação com retenção de 1 ano e termo de uso; todas as alterações são auditadas |
| Descontos | Não implementado | Desconto percentual de até 20%, exclusivo do GERENTE e só antes da saída |
| Entrada e saída | Sem controle de horários e transições | Registro de data/hora e responsável na entrada e na saída (com o entregador, na entrega); transições inválidas e alterações após o cancelamento bloqueadas |
| Cancelamento | Não existia | Exclusivo do GERENTE, antes da saída, com motivo obrigatório e dados históricos preservados |
| Auditoria | Não implementada | Entidade, repositório e serviço próprios, coleção só de inserção, consulta restrita a ADMIN e GERENTE |
| Autorização | Sem diferenciação por perfil; CSRF desativado | Regras por rota em `SecurityConfig` (o que não está listado é negado), `@PreAuthorize` nos serviços, CSRF ligado, respostas 401 e 403 tratadas e acessos negados registrados em log |
| Organização | Um único `HomeController` para todas as telas | Controllers separados por assunto, DTOs de requisição, exceções de negócio com tratador único e telas de pedido, auditoria e cadastros |
| Testes | Nenhum | 303 testes automatizados (unitários e de integração) |

Foram preservados: a arquitetura em camadas, as tecnologias (Java, Spring Boot, Spring Security, Thymeleaf e MongoDB), o tema AdminLTE, o layout com sidebar, a consulta de CEP pelo ViaCEP e o cadastro de usuários e de clientes.

### Cadastros que estavam incompletos no projeto original
O projeto recebido já exibia na sidebar os itens **Funcionários**, **Entregadores** e **Horário de Funcionamento** (e criava a coleção `horariosAtendimento`), mas sem telas nem regras. Foram concluídos, mantendo a arquitetura em camadas:

- **Funcionários** (`/funcionarios`): cadastro da equipe (nome, cargo, telefone, situação), separado do login dos usuários.
- **Entregadores** (`/entregadores`): nome, telefone, veículo e situação; o entregador ativo é escolhido na saída do pedido por entrega.
- **Horário de Funcionamento** (`/horarios`): grade semanal (atende, abre, fecha; fechamento antes da abertura significa que passa da meia-noite), exibida na home como "Aberto agora" ou "Fechado agora". Não bloqueia pedidos.
- **Produtos** (`/produtos`): a tela original estava quebrada (faltava o fragmento da página e os botões chamavam a API por GET); agora lista, cria, edita, ativa/inativa e remove.

Todos seguem a mesma regra de acesso: todos os perfis consultam e somente o GERENTE altera.

### Extra — ciclo de vida do cliente
- O cliente **nunca é excluído na hora**: o GERENTE apenas o inativa. Os dados ficam retidos por **1 ano** (`retidoAte`) e uma rotina agendada (`ExpurgoDeClientes`, todo dia às 03:00, configurável em `pediupatrao.expurgo.cron`) apaga os cadastros vencidos, registrando a auditoria como usuário `sistema`.
- Cliente inativo não recebe novos pedidos nem alterações.
- Ao cadastrar um cliente pela tela, aparece o aviso (toast) "Termos de uso encaminhados ao cliente". Neste contexto o aviso é apenas ilustrativo: nenhum e-mail é enviado.
- O cadastro exige o aceite de um termo de uso (checkbox marcado pelo atendente, confirmando que o cliente foi informado e concorda; o cliente recebe uma cópia), com data/hora do aceite gravada e auditada. O termo informa que, após a inativação, permanecem armazenadas por 1 ano apenas as informações relativas aos pedidos feitos. Clientes antigos ficam "sem aceite" até o atendente registrá-lo.

## Estrutura

```
com.umc.pediupatrao
├── config        SecurityConfig, SemeadorDeContas, SemeadorDeHorarios, RelogioConfig, MongoConversoesConfig, AgendamentoConfig
├── controller    APIs REST (/api/...)
│   └── tela      controllers das páginas Thymeleaf
├── dto           objetos de requisição e formulários
├── entity        documentos do MongoDB e enums
├── exception     exceções de negócio e tratador de erros da API
├── repository    interfaces Spring Data
├── security      usuário autenticado, tratamento de acesso negado
└── service       regras de negócio, auditores e expurgo
```

Princípios aplicados: regras no serviço e autorização no backend; DTOs em vez de entidades no corpo das requisições; injeção por construtor; enums no lugar de textos livres; exceções de domínio tratadas em um único ponto; nenhum comentário no código.

## Como demonstrar o bloqueio no backend

As telas escondem botões por perfil, mas a validação real é do servidor. Com o usuário logado, abra o console do navegador (F12) numa página do sistema e chame a API diretamente (o token CSRF está nas meta tags da página):

```js
const csrf = { [document.querySelector('meta[name=_csrf_header]').content]: document.querySelector('meta[name=_csrf]').content };
fetch('/api/pedidos/ID_DO_PEDIDO/cancelar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', ...csrf },
  body: JSON.stringify({ motivo: 'CLIENTE_DESISTIU' })
}).then(r => console.log(r.status));
```

Logado como `atendente01`, o resultado é `403`; como `gerente01`, `200`. Sem login, qualquer `/api/**` responde `401`.

## Decisões de projeto

| Tema | Decisão |
|---|---|
| Quem avança o status | ATENDENTE e GERENTE; o ADMIN só consulta pedidos. |
| Clientes | ATENDENTE cadastra e altera; GERENTE consulta e inativa; ADMIN não acessa. |
| Saída | `tipoSaida` (ENTREGA ou RETIRADA) é definido no cadastro do pedido e não muda. |
| Desconto | Substitui o anterior, incide sobre o subtotal, máximo de 20%. |
| Cancelamento | Motivos pré-definidos; "outro" exige observação (mínimo de 10 caracteres). |
| Datas e dinheiro | `LocalDateTime` no fuso America/Sao_Paulo; `BigDecimal` gravado como Decimal128. |
| Auditoria de cliente | Nome, telefone e endereço com valores anterior e novo completos; consulta restrita a ADMIN e GERENTE. |
| Exclusão de cliente | Substituída por inativação (só GERENTE), com retenção de 1 ano e expurgo automático; a inativação é a "exclusão" lógica. |
| Produtos | Somente o GERENTE cria, edita, inativa e exclui; todos consultam. |

## Limitações conhecidas

- O MongoDB local é *standalone* e não tem transações entre documentos. A auditoria é gravada logo após o salvamento do pedido ou cliente, no mesmo método de serviço; se ela falhar, a exceção sobe e a operação é reportada como erro, mas a alteração já gravada não é desfeita.
- O Bootstrap, os ícones e a consulta de CEP (ViaCEP) usam serviços externos (CDN e API online) e dependem de internet; o restante do sistema funciona offline.
- Pedidos criados antes desta versão usam outro modelo de dados e não são compatíveis; o banco local deve ser limpo se houver pedidos antigos.

## Testes

A suíte automatizada cobre: transições de status, cálculo de valores e limites de desconto, cancelamento, regras de usuários (autoelevação, último ADMIN), matriz perfil × rota (200, 401 e 403), CSRF, auditoria (campos, valores anterior e novo, perfil, imutabilidade e filtros), renderização das telas por perfil, o ciclo de vida do cliente e os cadastros de funcionários, entregadores e horários.

## Vídeo de demonstração

O vídeo mostra: autenticação dos três perfis; bloqueio de operações não autorizadas (inclusive chamadas diretas à API); entrada, transições e saída de pedidos; cancelamento e desconto pelo GERENTE; e a consulta aos registros de auditoria resultantes.

