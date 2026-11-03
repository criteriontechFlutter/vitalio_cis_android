/**
 * Publishes the app's feature switches to Remote Config.
 *
 * The Android admin page (Feature Flags, only for the admin login) can't write Remote Config —
 * the client SDK is read-only — so it saves each switch to the Firestore doc
 * `cis_app_config/feature_flags`. This function copies every `cis_feature_*` boolean from that doc
 * into the Remote Config template and publishes it. Apps listen for Remote Config updates in real
 * time (FeatureFlags.kt), so a switch reaches open apps within seconds.
 */
const { initializeApp } = require("firebase-admin/app");const { getRemoteConfig } = require("firebase-admin/remote-config");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");

initializeApp();

const DOC_PATH = "cis_app_config/feature_flags";
const KEY_PREFIX = "cis_feature_";
const GROUP = "CIS feature flags";
const MAX_ATTEMPTS = 3;

exports.publishFeatureFlags = onDocumentWritten(DOC_PATH, async (event) => {
  const ref = event.data?.after?.ref;
  if (!ref) return;

  const rc = getRemoteConfig();
  // Quick toggles run several of these at once and they can finish out of order, and the
  // template is shared with the project's other apps, so a publish can fail on its ETag. Each
  // attempt re-reads both the doc and the template, so the last publish always carries the
  // switches as they are now, not as they were when this event fired.
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    const data = (await ref.get()).data();
    if (!data) return; // doc deleted: leave Remote Config as it is
    const flags = Object.entries(data).filter(
      ([key, value]) => key.startsWith(KEY_PREFIX) && typeof value === "boolean",
    );
    if (flags.length === 0) return;

    const template = await rc.getTemplate();
    template.parameterGroups[GROUP] ??= {
      description: "Main modules of the CIS Android app. Set from the in-app admin page.",
      parameters: {},
    };
    const group = template.parameterGroups[GROUP].parameters;

    let changed = false;
    for (const [key, enabled] of flags) {
      // A key someone created at the top level in the console is updated where it is
      const params = key in template.parameters ? template.parameters : group;
      const value = String(enabled);
      const current = params[key];
      const upToDate =
        current?.defaultValue?.value === value &&
        Object.keys(current.conditionalValues ?? {}).length === 0;
      if (upToDate) continue;
      // No conditional values: the admin's switch applies to every user
      params[key] = {
        defaultValue: { value },
        valueType: "BOOLEAN",
        description: current?.description ?? "CIS app feature switch (true = shown)",
      };
      changed = true;
    }
    if (!changed) return;

    try {
      await rc.publishTemplate(template);
      logger.info("Published feature flags", {
        updatedBy: data.updatedBy,
        flags: Object.fromEntries(flags),
      });
      return;
    } catch (err) {
      logger.warn(`Publishing feature flags failed (attempt ${attempt})`, err);
      if (attempt === MAX_ATTEMPTS) throw err;
    }
  }
});
