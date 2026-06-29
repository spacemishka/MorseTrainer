# Morse Trainer - Software Requirements Specification

## 1. Purpose

The purpose of this application is to teach Morse code efficiently using the Koch Method. The trainer shall focus on auditory recognition of Morse characters as sound patterns rather than counting dots and dashes.

The application should guide users from complete beginner level to practical Morse proficiency and reinforce learned skills through interactive games and simulations.

---

# 2. Target Audience

* Amateur radio operators
* SWLs (Short Wave Listeners)
* Maritime and aviation enthusiasts
* Military history enthusiasts
* Users interested in cognitive training
* Students preparing for Morse code examinations

---

# 3. Learning Methodology

## 3.1 Koch Method

The application shall implement the Koch Method as the primary learning approach.

Characteristics:

* Start with only two characters.
* Characters are sent at full speed from the beginning.
* New characters are introduced only after reaching a predefined success rate.
* Learners focus on recognizing sound patterns instead of counting elements.

### Default Settings

| Parameter                    | Value       |
| ---------------------------- | ----------- |
| Character Speed              | 20 WPM      |
| Effective Speed (Farnsworth) | 10–15 WPM   |
| Promotion Threshold          | 90% Correct |

### Koch Character Sequence

Default sequence:

```text
K M
R S
U A
P T
L O
W I
. N
J E
F 0
Y V
G 5
Q 9
Z H
3 8
B ?
4 7
C 1
D 6
X /
= 2
```

The sequence shall be configurable.

---

# 4. Functional Requirements

## 4.1 User Profiles

Users shall be able to:

* Create multiple learning profiles
* Start a new training program
* Resume existing training
* Backup and restore progress
* View personal statistics

Stored data:

* Current Koch lesson
* Accuracy rates
* Problem characters
* Total training time
* Achievements and badges

---

## 4.2 Koch Lesson Mode

### Description

The system plays Morse characters and the user identifies them.

### Workflow

1. Play Morse character.
2. User enters the recognized character.
3. Immediate feedback is provided.
4. Statistics are updated.

### Configurable Options

* Character speed
* Farnsworth spacing
* Number of characters per lesson
* Randomized order
* Adaptive repetition of difficult characters

---

## 4.3 Word Training Mode

Unlocked after learning a configurable minimum number of characters.

Training content:

* Random words
* Amateur radio abbreviations
* Q-codes
* Callsigns
* Numbers

Examples:

```text
CQ
TEST
QTH
73
BERLIN
```

---

## 4.4 Free Text Training

For advanced learners.

Supported sources:

* Built-in texts
* User-defined texts
* News articles
* Technical articles
* Amateur radio content

---

# 5. Gamification and Reinforcement Modules

## 5.1 Morse Hangman

### Objective

Reinforce recognition of learned characters.

### Gameplay

A partially hidden word is displayed.

Example:

```text
_ A _ I O
```

The user hears:

```text
-...
```

The correct answer:

```text
B
```

Updated word:

```text
B A _ I O
```

### Difficulty Levels

* Beginner
* Intermediate
* Advanced

### Variations

* Single-character clues
* Multiple-character clues
* Entire syllables
* Hints enabled/disabled

---

## 5.2 Morse Treasure Hunt

### Objective

Solve Morse-coded clues to advance through a sequence of challenges.

### Example

Received:

```text
.... .- .-.. .-.. ---
```

Answer:

```text
HELLO
```

### Content Types

* Words
* Numbers
* Coordinates
* Radio messages
* Puzzle clues

---

## 5.3 Morse Typing Challenge

### Objective

Improve recognition speed.

### Gameplay

* Continuous stream of Morse characters
* User enters recognized characters
* Points awarded for correct answers
* Combo multiplier for streaks
* High score tracking

### Metrics

* Accuracy
* Characters per minute
* Longest streak
* Total score

---

## 5.4 Morse Memory

### Objective

Strengthen character association.

### Matching Types

* Character ↔ Morse
* Audio ↔ Character
* Audio ↔ Morse Pattern

Examples:

| Card A | Card B |
| ------ | ------ |
| A      | .-     |
| R      | .-.    |
| 7      | --...  |

---

## 5.5 Morse Word Builder

### Objective

Practice assembling words from Morse characters.

Example sequence:

```text
-...
.
.-.
.-..
.. -.
```

Expected result:

```text
BERLIN
```

### Scoring

* Completion speed
* Accuracy
* Hint usage

---

## 5.6 Amateur Radio QSO Simulator

### Objective

Prepare users for real-world Morse communication.

### Scenarios

* Calling CQ
* Contest exchanges
* Signal reports
* Q-code exchanges
* Casual QSOs

Example:

```text
CQ CQ DE DL1ABC
```

Users must respond appropriately.

---

# 6. Adaptive Learning System

The application shall track performance for each Morse character.

Example statistics:

| Character | Accuracy |
| --------- | -------- |
| K         | 98%      |
| R         | 95%      |
| Y         | 61%      |

Characters with low accuracy shall:

* Appear more frequently
* Be prioritized in lessons
* Be emphasized in games
* Delay introduction of new characters if necessary

---

# 7. Audio Requirements

## 7.1 Morse Generator

Configurable parameters:

* Tone frequency (300–1000 Hz)
* Volume
* Character speed
* Farnsworth spacing

---

## 7.2 Audio Profiles

Supported playback modes:

* Pure sine wave
* Amateur radio transceiver simulation
* Background noise
* QRM simulation
* QSB fading simulation

These modes are intended for advanced learners.

---

# 8. Statistics and Progress Tracking

The application shall display:

* Total training time
* Current Koch lesson
* Overall accuracy
* Best streak
* Problem characters
* Learning progression
* Game statistics

### Visualizations

* Accuracy trend
* Character mastery chart
* Speed improvement chart
* Lesson progression chart

---

# 9. Achievement System

Achievements may include:

* First lesson completed
* First word copied correctly
* 1,000 characters recognized
* Seven-day training streak
* First successful QSO simulation
* Koch course completion

Badges and milestones should motivate continued practice.

---

# 10. Technical Requirements

---

## Offline Capability

The application shall function completely offline after installation.

---

## Data Storage

Preferred local database:

* recommended DB for Android

---

## Import / Export

Supported formats:

* CSV
* JSON

---

# 11. Future Enhancements

Potential future features:
* CW decoder comparison mode
* Callsign training database
* Contest training scenarios (optional)
* Custom lesson creation

---

# 12. Success Criteria

A learner shall be considered proficient when they can:

* Recognize all supported Morse characters
* Maintain at least 90% accuracy
* Copy words at 15 WPM or faster
* Complete simulated QSOs successfully
* Finish the complete Koch training sequence

The application should support users from their first Morse character through practical real-world Morse communication.
