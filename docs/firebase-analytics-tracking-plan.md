# Mystery Number Firebase Analytics Tracking Plan

Status: Draft for approval

## Objective

Measure the core player funnel without collecting personal data:

- App launch and screen flow.
- Home to gameplay conversion.
- Level difficulty, progress, fail points, hint use, and extra-time recovery.
- Ad-free purchase funnel.
- Settings interactions that affect retention or UX.

## Implementation Status

| Area | Event | Status |
| --- | --- | --- |
| Firebase SDK wiring | `FirebaseAnalytics` dependency and tracker wrapper | Implemented |
| Screen tracking | `screen_view` and `{screen}_show`, for example `main_show` | Implemented |
| Session | `app_session_start` | Implemented |
| Home | `main_play`, `main_settings`, `play_tap`, `navigation_tap` | Implemented |
| Gameplay | `game_lv`, `game_swap`, `game_check`, `game_hint`, `level_start`, `order_check`, `hint_use`, `level_complete`, `game_over` | Implemented |
| Extra time | `extra_time_request`, `extra_time_result` | Implemented |
| Premium | `premium_continue`, `premium_close`, `premium_purchase_start`, `premium_purchase_success` | Implemented |
| Settings | `settings_back`, `settings_premium`, `settings_sound`, `settings_rate`, `settings_policy`, `settings_sound_toggle`, `navigation_tap` | Implemented |
| Ads diagnostics | Banner/app-open/rewarded load and show errors | Proposed |
| Economy | Coin balance checkpoints and coin sources/sinks | Proposed |
| Retention | Daily streak / returning player cohorts | Proposed |

## Event Dictionary

| Event | Trigger | Key params |
| --- | --- | --- |
| `screen_view` | Activity resumes | `screen_name`, `screen_class` |
| `{screen}_show` | Activity resumes, for example `main_show`, `game_show`, `settings_show` | none |
| `app_session_start` | Application starts | `session_number`, `is_ad_free` |
| `{screen}_{button}` | User taps a screen button, for example `main_play`, `game_back`, `settings_premium` | none |
| `game_swap` | Player swaps two numbers manually | `level`, `level_name` |
| `game_lv` | Player reaches a level. Level 1 fires when a new game screen starts; next levels fire after passing the previous level. Restored saves do not refire it. | `level`, `level_name` |
| `play_tap` | Player taps Play / Continue | `has_saved_game` |
| `navigation_tap` | Settings, Premium, Rate, Policy navigation | `action` |
| `level_start` | New or restored level begins | `level`, `level_name`, `digits`, `adjacent_rule`, `is_restored` |
| `order_check` | Player checks answer | `level`, `level_name`, `correct_count`, `total_digits`, `checks_used` |
| `hint_use` | Player uses a hint | `level`, `level_name`, `hints_used`, `hints_remaining` |
| `level_complete` | Player solves level | `level`, `level_name`, `score`, `level_score`, `stars`, `time_left`, `checks_used`, `hints_used`, `combo` |
| `game_over` | Timer reaches zero | `level`, `level_name`, `score`, `checks_used`, `hints_used` |
| `extra_time_request` | Player taps ad or coin extra time | `method`, `level`, `level_name`, `coin_balance` |
| `extra_time_result` | Extra-time path completes/fails | `method`, `level`, `level_name`, `granted`, `seconds` |

`level` is numeric for Custom metric reporting. `level_name` is text, for example `lv3`, for Custom dimension filtering.
| `premium_purchase_start` | Billing flow launches | none |
| `premium_purchase_success` | Remove Ads is granted | `product_id`, `was_already_ad_free` |
| `settings_sound_toggle` | Sound switch changes | `is_enabled` |

## Proposed Next Events

Add these after reviewing dashboard noise and product priorities:

| Event | Why | Params |
| --- | --- | --- |
| `ad_load_result` | Debug fill-rate and integration issues | `ad_format`, `placement`, `success`, `error_code` |
| `ad_show_result` | Debug show failures and revenue funnel | `ad_format`, `placement`, `success`, `error_code` |
| `coin_balance_checkpoint` | Understand economy health | `source`, `coin_balance`, `delta` |
| `saved_game_resume` | Measure continue feature usage | `level`, `time_left`, `score` |
| `all_levels_complete` | Track full game completion | `score`, `best_score` |

## Approval Questions

- Should `order_check` be kept, or reduced to only failed checks per level to lower event volume?
- Should swap-level telemetry be added later, or avoided because it will be noisy?
- Should ad diagnostics include error messages, or only numeric error codes?
- Should we create Firebase audiences for `is_ad_free`, high-level players, and repeated game-over users?

## Firebase Console Setup

1. Create/select the Firebase project.
2. Add Android app with package name `com.swapnumber.puzzle`.
3. Download `google-services.json`.
4. Place it at `app/google-services.json`.
5. Rebuild the app; the Google Services plugin is applied automatically when that file exists.
