# Hegemony Golden Test Cases (v1.2)

## Scope

- source_document: `hegemony_base_rules`, `hegemony_crisis_and_control_expansion`
- source_section: `golden regression scenarios for setup + legality + automa + voting`
- source_status: `derived_from_rules`

This document is the human-readable companion for `specs/test-cases.yaml`.

## Canonical Scenario Set

1. `setup_2_players`
2. `setup_3_players`
3. `setup_4_players`
4. `illegal_non_adjacent_policy_change`
5. `illegal_proposal_on_occupied_policy`
6. `illegal_partial_enterprise_fill`
7. `illegal_sell_enterprise_with_tied_contract_workers`
8. `union_closes_when_workers_drop_below_4`
9. `demonstration_trigger_case`
10. `voting_all_same_side`
11. `voting_empty_bag_case`
12. `automa_complex_priority_shift_case`
13. `automa_last_round_policy_restriction_case`

## Expected Validation Focus

- Setup scenarios verify canonical constants for player composition, policy courses, treasury, public services, and initial voting bag.
- Illegal scenarios verify deterministic failure reason codes:
  - `BILL_TARGET_COURSE_NOT_ADJACENT`
  - `BILL_TARGET_POLICY_ALREADY_HAS_PROPOSAL_TOKEN`
  - `ENTERPRISE_PARTIAL_FILL_NOT_ALLOWED`
  - `ENTERPRISE_HAS_TIED_CONTRACT_WORKERS_CANNOT_SELL`
- Transition scenarios verify union closure when industry worker count drops below 4 and demonstration token transition behavior.
- Voting scenarios verify:
  - draw/discard of 5 cubes even with unanimous vote
  - empty-bag flow with two refill attempts before draw
- Automa scenarios verify:
  - complex priority shift mechanics (checks, insertion/shift directions, row collapse)
  - round-5 restrictions (`policy_7` prohibition, migration priority removal, full influence spending)

## Unknown Handling in Tests

- runtime_randomized:
  - revealed deck card identities at setup
  - voting bag draw order
- not_required_for_rules_engine_v1:
  - narrative phrasing variants
- still_needs_source_extraction:
  - exact numeric demonstration trigger threshold

No new numeric constants were introduced in this test layer.
