"""Levels and rewards.

The numbers must not change: the levels of all persons are derived from them.
The old application calculated with 32-bit floats, which is repeated here so
that every level and bound comes out the same.
"""
import math
import struct

MAX_LEVEL = 100
LEVELING_SCALE = 200
REWARD_FACTOR = 50

# Part of the reward of an activity that the author of the assignment gets
OWNER_REWARD_FACTOR = 0.1


def _float32(value):
    return struct.unpack("f", struct.pack("f", value))[0]


LEVELING_POWER = _float32(0.7)
REWARD_POWER = _float32(0.7)


def get_level(xp):
    return int(min(MAX_LEVEL, 1 + math.pow(_float32(max(xp, 0) / LEVELING_SCALE), LEVELING_POWER)))


def get_min_xp_for_level(level):
    return int(math.pow(min(level, MAX_LEVEL) - 1, _float32(1 / LEVELING_POWER)) * LEVELING_SCALE)


def get_level_progress(xp):
    """Returns how far the person is on the way to the next level, from 0 to 1."""
    level = get_level(xp)
    current = get_min_xp_for_level(level)
    following = get_min_xp_for_level(level + 1)
    if following == current:
        return 0.0
    return _float32((xp - current) / _float32(following - current))


def get_initial_assignment_reward(level):
    """XP that a person of this level puts on a new assignment or adds with a boost."""
    return int(REWARD_FACTOR * math.pow(level, REWARD_POWER))


def get_activity_reward(max_reward, likes, dislikes):
    """XP for the author of an activity when the rating period is over."""
    like_ratio = 0.5 if likes + dislikes == 0 else _float32(likes / _float32(likes + dislikes))
    return math.ceil(_float32(like_ratio * max_reward))


def get_owner_reward(activity_reward):
    return int(_float32(_float32(OWNER_REWARD_FACTOR) * activity_reward))
