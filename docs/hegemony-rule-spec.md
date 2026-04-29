# Hegemony Canonical Rule Spec (Base + Crisis and Control)

## Scope

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `document scope and allowed sources`
- source_status: `explicit_from_rules`

This specification uses only:
- Hegemony base rules
- Crisis and Control expansion

No external sources were used.

If a value is not explicitly provided in the task and cannot be unambiguously extracted from the rules text available in this task, it is marked as `UNKNOWN`.

## Normalization

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `policy course notation normalization`
- source_status: `derived_from_rules`
- derived_logic: machine fields are constrained to canonical tokens `A | B | C`; human-readable companion keeps same canonical token plus Cyrillic label `А | Б | В`.

Machine-readable policy courses use only `A | B | C`.
In this human-readable document, the same courses are shown as `A | B | C` (`А | Б | В`).

## Active Classes by Player Count

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `setup active classes by player count`
- source_status: `explicit_from_rules`

- 2 players -> `worker`, `capitalist`
- 3 players -> `worker`, `middle_class`, `capitalist`
- 4 players -> `worker`, `middle_class`, `capitalist`, `state`

Note: even when `state` is not a player, state sector, treasury, and public services still exist on the board.

## Initial Policy Courses

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `setup initial policy courses`
- source_status: `explicit_from_rules`

| Policy ID | Name (RU) | Start Course |
|---|---|---|
| policy_1_fiscal | фискальная политика | C (В) |
| policy_2_labor_market | политика на рынке труда | B (Б) |
| policy_3_taxation | налоговая политика | A (А) |
| policy_4_healthcare_and_benefits | социальная политика в сфере здравоохранения и льгот | B (Б) |
| policy_5_education | социальная политика в сфере образования | C (В) |
| policy_6_foreign_trade | внешнеторговая политика | B (Б) |
| policy_7_immigration | миграционная политика | B (Б) |

## Initial Board State (Canonical)

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `setup board initialization and state-sector first-row placement`
- source_status: `derived_from_rules`
- derived_logic: first row is mapped to 3 public services; healthcare/education names are partially confirmed by worker setup; media state-enterprise exact name is not explicitly provided in task text.

- Capitalist starting enterprises on board:
  - `supermarket`, `shopping_center`, `college`, `polyclinic`
  - salary marker on each: `salary_level = 2`
- State enterprises setup:
  - remove 3 cards not matching player count by diamond cost
  - place 3 starting state enterprises in first state-sector row under corresponding public service spaces
  - first-row name trace:
    - healthcare: `state_hospital` (2 players), `university_hospital` (3-4 players) - derived
    - education: `state_university` confirmed for 2 players, 3-4 specific state education enterprise name remains `UNKNOWN`
    - media influence enterprise name remains `UNKNOWN`
  - salary marker on each starting state enterprise: `salary_level = 2`
  - second-row state enterprise cards by type: face down
  - remaining 3 state enterprise cards in third row: face down
- For 3-4 players, additionally place middle-class starting enterprises:
  - `minimarket`, `private_clinic`
  - salary marker on each: `salary_level = 2`
- Deal deck:
  - shuffle, reveal top card, place on matching board space
- Export deck:
  - shuffle, reveal top card, place on export space
  - import marker position: `U2`
- Migrant deck:
  - shuffle and place on board space
- `state_treasury = 120`
- Public services start:
  - 2 players: `healthcare=5`, `education=5`, `media_influence=3`
  - 3 or 4 players: `healthcare=6`, `education=6`, `media_influence=4`
- `round_marker = 1`
- `tax_multiplier = 5`
- `vp_marker_each_player = 0`
- Voting bag setup:
  - cubes of all 3 colors near board
  - bag starts with: `worker=8`, `middle_class=8`, `capitalist=8`
  - applies even in 2-player game

## Initial Player Setup Constants

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `player board setup by class`
- source_status: `explicit_from_rules`

### Worker

- `population_marker = 10 workers`
- `welfare = 0`
- `money = 30`
- `personal_influence = 1`
- `proposal_tokens = 3 red`
- `voting_token = 1`
- `hand_size = 7`

### Capitalist

- `revenue = 120`
- `wealth_marker = near wealth track`
- resources: `food=1`, `luxury=2`, `education=2`, `influence=1`
- prices: `food_price=12`, `luxury_price=8`, `education_price=8`, `influence_price=8`
- `free_trade_zone_token = 1`
- `proposal_tokens = 3 blue`
- `voting_token = 1`
- `hand_size = 7`

### State (4 players only)

- legitimacy: `worker=2`, `middle_class=2`, `capitalist=2`
- `personal_influence = 1`
- `proposal_tokens = 3 gray`
- `voting_token = 1`
- `hand_size = 7`

### Middle Class (3-4 players only)

- `population_marker = 10 workers`
- `welfare = 0`
- `money = 40`
- `personal_influence = 1`
- resources: `food=1`, `healthcare=1`
- prices: `food_price=12`, `luxury_price=8`, `healthcare_price=8`, `education_price=8`
- `proposal_tokens = 3 yellow`
- `voting_token = 1`
- `hand_size = 7`

## Canonical Round Structure

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `round flow and per-phase order`
- source_status: `explicit_from_rules`

- `total_rounds = 5`
- Phases:
  1. `preparation`
  2. `actions`
  3. `production`
  4. `voting`
  5. `scoring`
- In round 1, phase `preparation` is skipped.

Action phase order:
1. `worker`
2. `middle_class`
3. `capitalist`
4. `state`

- Each player performs `5` turns in action phase.
- After that, player keeps `2` cards in hand for next round.

Production order (reverse):
1. `state`
2. `capitalist`
3. `middle_class`
4. `worker`

Production stages:
- `produce_goods_and_services`
- `satisfy_needs`
- `check_imf_intervention`
- `pay_taxes`

IMF canonical flow:
- check state loans against current fiscal policy threshold
- if threshold exceeded, attempt to repay `55` per loan
- if after repayment state still exceeds allowed loans, IMF intervention occurs

If mini-expansion `Антикризисные меры` is enabled, standard IMF flow is replaced with:
1. discard specified proposal tokens
2. change specified policy courses
3. resolve additional effects in order
4. repay loans
5. reduce legitimacy

Lock-token rule in anti-crisis flow:
- if anti-crisis card prohibits policy-course changes, place lock token on that policy
- while lock token is present, players cannot propose bills for that policy
- on next IMF check, remove all lock tokens

Voting stages:
- `refill_bag`
- `resolve_votes`

If no bill was proposed in actions phase, voting phase is skipped.

## Voting Mechanics (Start Rules)

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `voting refill and draw procedure`
- source_status: `explicit_from_rules`

Refill formulas:
- worker adds `ceil(population / 2)`
- capitalist adds `ceil(functioning_enterprises / 2)`
- middle_class adds `ceil(max(population / 2, functioning_enterprises / 2))`

Additional rules:
- in 2-player game, add `5` middle-class cubes to the bag
- state does not add cubes; instead gains influence equal to minimum legitimacy among classes
- draw `5` cubes for each vote
- if all players vote the same, still draw and discard `5` cubes
- if bag is empty, execute refill step twice, then draw

## Mandatory Rule Constraints

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `mandatory action constraints`
- source_status: `explicit_from_rules`

- bill can be proposed only to adjacent course
- cannot propose bill to policy with existing bill token
- enterprise is either fully functioning or fully non-functioning
- partial staffing is not allowed
- full/non-full enterprise check occurs after assign-workers action resolution
- skilled slots require matching skilled workers by industry
- worker under tied contract cannot be reassigned
- enterprise with tied-contract workers cannot be sold
- union can be created only if industry has at least 4 occupied workers
- if industry drops below 4 workers, union worker immediately goes to unemployment
- buy goods/services action chooses exactly one resource_or_service_type
- buy action uses at most two suppliers
- from each supplier, buy no more than current population

## Automa Canon

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `automa setup simple mode complex mode and round-5 exceptions`
- source_status: `explicit_from_rules`

- Available automas: `worker_automa`, `middle_class_automa`, `capitalist_automa`
- When using automa, setup game as full composition
- Leave replaced human class action cards in box
- Automa uses dedicated automa cards

Simple mode:
- `30` automa action cards
- remove instruction cards with header containing `CHECK`

Complex mode checks and priority movement (canonical):
- top 4 symbols on automa card are checks, not actions
- checks execute strictly left to right
- first-check condition evaluation includes automa card bonus
- on successful check, move referenced action priority card upward by instructed row count
- if target row already has cards, insert moved action card to the left
- when extracting action card from a row, remaining cards shift right toward row token
- policy priority cards move analogously, but insert to the right; remaining row cards shift left
- if policy priority card is already set aside, it is not moved
- after all checks, collapse empty rows top-down independently for action and policy priority zones
- execute highest-priority action; on tie choose card closest to row token
- if selected action cannot be performed, try next priority; if also impossible, do `APPLY_POLITICAL_PRESSURE`
- after performing an action, its priority card moves down 2 rows
- if executed action was not topmost but next available, only executed action card moves down

Last-round automa rules:
- automa does not propose bills on `policy_7`
- in complex mode, remove migration policy priority at start of round 5
- automa proposes on `policy_6` only if it can call extraordinary vote
- in last-round voting, all automas spend all influence

## UNKNOWN Fields (Not Invented)

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `values requiring deterministic draw/reveal or missing explicit naming in task text`
- source_status: `unknown`

- identities of revealed top cards from deal/export/migrant/event/agenda decks at setup
- exact first-row media state-enterprise name
- exact 3-4-player education state-enterprise name in first row
- initial concrete hand card identities for each class (`hand_size` is known, card IDs are not)

Reason: these values/procedures are not explicitly provided in the task as deterministic literals and cannot be derived unambiguously without additional canonical rule text.

## UNKNOWN Classification (v1.2)

- runtime_randomized:
  - revealed top-card identities from shuffled decks
  - initial concrete hand card identities
- not_required_for_rules_engine_v1:
  - none currently in this document-level list
- still_needs_source_extraction:
  - exact media state-enterprise canonical name
  - exact 3-4 player education state-enterprise canonical name

## Resolved in v1.2

- `functioning_enterprises_exact_definition` removed from UNKNOWN:
  - canonical engine-level definition now uses post-assign-workers binary status:
  - `count_of_enterprises_with_fully_functioning_state_true_after_post_assignment_check`
