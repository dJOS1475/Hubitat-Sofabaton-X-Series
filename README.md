# Hubitat-Sofabaton-X-Series

Hubitat driver for the Sofabaton X Series v1.7

Lets a Sofabaton X Series remote trigger Hubitat automations. When you start or stop an
activity on the remote, it sends a value to Hubitat over a local HTTP PUT, and the driver
turns that into a button press you can use as a rule trigger.

**Compatible hardware:** X1S (minimum supported), X2

> **Note:** this is one-way, remote → Hubitat only. See
> [Triggering Sofabaton from Hubitat](#triggering-sofabaton-from-hubitat) for the other direction.

## Installation

1. In Hubitat, go to **Drivers Code** → **New Driver** → **Import**.
2. Paste this URL and click Import, then Save:
   ```
   https://raw.githubusercontent.com/dJOS1475/Hubitat-Sofabaton-X-Series/refs/heads/main/Sofabaton_Driver.groovy
   ```
3. Go to **Devices** → **Add Device** → **Virtual**, give it a name, and set the Type to
   **Sofabaton X Series**.
4. Set a static DHCP reservation for your Sofabaton hub on your router.
5. Open the new device and enter that reserved address in **Remote IP Address**, then Save
   Preferences.

## How it works

The driver accepts three kinds of input in the request body:

| Body value | What fires |
|---|---|
| `on` or `off` | A switch event (always active, no configuration needed) |
| `1` – `10` | Button 1–10 |
| Any string you configure | Button 11–20, matched against your User Definable slots |

Because Hubitat's `PushableButton` capability defines `pushed` as a **number**, every input
fires a numeric button event — a string can't be sent as a button number. String activities
are handled by mapping them to buttons 11–20:

| User Definable slot | Fires button |
|---|---|
| User 1 | 11 |
| User 2 | 12 |
| … | … |
| User 10 | 20 |

If you'd rather trigger on the text itself, the driver also exposes two string attributes on
every press — see [Triggering on the string](#triggering-on-the-string-instead).

## Configuring the remote

In the Sofabaton mobile app:

1. Go to **Devices** → **Add Device** → **Wi-Fi**.
2. Tap **Create a virtual device for IP control** at the bottom.
3. Enter the URL `http://[your Hubitat IP]:39501/`
4. Set the request method to **PUT**.
5. Leave Content Type and Additional Headers blank.
6. In the **Body** field, enter either a number `1`–`10`, or any string such as `watchTV`.
7. Repeat for each activity, using a unique value each time.

## Configuring the driver

### Numeric buttons

The **Numeric Buttons** preferences (`1:` through `10:`) are optional labels used only in the
logs, to remind you what each number represents. Button 1–10 fire whether or not you label them.

### User definable buttons

For string activities, fill in a **User Definable** slot with the string the remote sends.
Optionally add a pipe `|` and a friendly description:

```
watchTV|Watch TV
```

Matching is case insensitive, and surrounding whitespace is ignored. The description is used
in log messages and in the `lastButtonLabel` attribute.

## Using it in Rule Machine

### By button number

Create a rule with a **Button Device** trigger, pick your Sofabaton device, choose **pushed**,
and enter the button number — `1`–`10` for numeric activities, `11`–`20` for your user
definable ones.

### Triggering on the string instead

Every press also sets two custom attributes:

- **`lastButtonValue`** — the raw string the remote sent, e.g. `watchTV`
- **`lastButtonLabel`** — your configured description, e.g. `Watch TV`

To use these, create a rule with a **Custom Attribute** trigger, select the Sofabaton device,
pick `lastButtonValue` (or `lastButtonLabel`), and set the comparison to the value you want.

### On/off

Sending `on` or `off` in the body sets the device's switch state, so the device can be used
anywhere a switch is accepted.

## Triggering Sofabaton from Hubitat

This driver only handles remote → Hubitat. To go the other way, enable the API in the
Sofabaton mobile app, which exposes a webhook URL per activity. Paste that URL into a Rule
Machine action of type **Send HTTP GET**. No additional driver is needed.

## Troubleshooting

Turn on **Enable debug logging** in the device preferences, then trigger the activity and check
**Logs**. The driver logs the raw request body it receives, which is usually enough to spot the
problem.

- **`No match found for received body value: X`** — the body the remote sent doesn't match any
  User Definable slot, and isn't a number 1–10. Copy the exact value from the log into a slot.
- **Nothing appears in the logs at all** — the remote isn't reaching Hubitat. Check the URL is
  port `39501`, the method is PUT, and that the Remote IP Address preference matches your
  Sofabaton hub's actual (reserved) address.
- **Rules were working before v1.7 and now aren't** — string activities previously sent an
  invalid `pushed` value. Rebuild those rules against buttons 11–20, or against the
  `lastButtonValue` attribute.

## Credits

Originally published by Mike Maxwell (Hubitat Inc). Subsequent contributions from Gassgs,
SViel, and dJOS.
