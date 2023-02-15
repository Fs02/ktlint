package com.pinterest.ktlint.core.internal

/**
 * Provides policy to have consistent and restricted `id` field naming style.
 */
@Deprecated("Deprecated since ktlint 0.49.0. Remove when Rule and RuleSetProviderV2 have been removed.")
internal object IdNamingPolicyLegacy {
    private const val SIMPLE_ID_REGEX = "[a-z]+(-[a-z]+)*"
    private val RULE_ID_REGEX = "($SIMPLE_ID_REGEX:)?($SIMPLE_ID_REGEX)".toRegex()
    private val RULE_SET_ID_REGEX = "($SIMPLE_ID_REGEX)".toRegex()

    /**
     * Checks provided [ruleId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleId] name.
     */
    internal fun enforceRuleIdNaming(ruleId: String) =
        require(ruleId.matches(RULE_ID_REGEX)) { "Rule id '$ruleId' must match '${RULE_ID_REGEX.pattern}'" }

    /**
     * Checks provided [ruleSetId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleSetId] name.
     */
    internal fun enforceRuleSetIdNaming(ruleSetId: String) =
        require(ruleSetId.matches(RULE_SET_ID_REGEX)) { "RuleSet id '$ruleSetId' must match '${RULE_SET_ID_REGEX.pattern}'" }
}
