package com.idlerpg.game.data.local

/** Strong schema version for the logical SaveData field contract. */
data class SaveVersion(
    val value: Int
) : Comparable<SaveVersion> {

    init {
        require(value >= 0) {
            "SaveVersion cannot be negative: $value"
        }
    }

    override fun compareTo(other: SaveVersion): Int =
        value.compareTo(other.value)

    companion object {
        /** Synthetic pre-release version used only by migration verification scenarios. */
        val V0: SaveVersion = SaveVersion(0)

        /** First persisted backend schema introduced by Foundation 15. */
        val V1: SaveVersion = SaveVersion(1)

        /** FBE-00 schema: manual-input/claim/Echo-purchase/capacity state ownership. */
        val V2: SaveVersion = SaveVersion(2)

        /** Multi-wave encounter runtime state. */
        val V3: SaveVersion = SaveVersion(3)

        /** Deterministic active/offline loot-filter policy. */
        val V4: SaveVersion = SaveVersion(4)

        /** Push/Farm automation mode and cleared-stage ownership. */
        val V5: SaveVersion = SaveVersion(5)

        /** Run-level mutually exclusive skill-evolution selections. */
        val V6: SaveVersion = SaveVersion(6)

        /** Chronicle-persistent player-facing hero identity. */
        val V7: SaveVersion = SaveVersion(7)

        /** Permanent Rebirth/Legacy point pools and stat allocations. */
        val V8: SaveVersion = SaveVersion(8)

        /** Explicit per-skill rank, mastery, and refinement tracks. */
        val V9: SaveVersion = SaveVersion(9)

        /** Bounded per-item enhancement failstack state. */
        val V10: SaveVersion = SaveVersion(10)

        /** Separates Normal combat and Legacy acquisition allocations. */
        val V11: SaveVersion = SaveVersion(11)

        val CURRENT: SaveVersion = V11
    }
}
