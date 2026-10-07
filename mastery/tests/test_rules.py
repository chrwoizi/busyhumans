from django.test import SimpleTestCase, TestCase

from mastery import balancing, sanitizer, search
from mastery.templatetags.mastery_tags import linkify
from mastery.tests import helpers
from mastery.viewer import ANONYMOUS


class BalancingTest(SimpleTestCase):
    """The numbers are those that the old site showed for the same XP."""

    def test_levels(self):
        self.assertEqual(balancing.get_level(0), 1)
        self.assertEqual([balancing.get_level(xp) for xp in (722, 784, 817)], [3, 3, 3])
        self.assertEqual(balancing.get_level(1448), 4)
        self.assertEqual(balancing.get_level(1478), 5)
        self.assertEqual(balancing.get_level(10 ** 9), 100)

    def test_bounds_of_a_level(self):
        self.assertEqual(balancing.get_min_xp_for_level(1), 0)
        self.assertEqual(balancing.get_min_xp_for_level(4), 960)
        self.assertEqual(balancing.get_min_xp_for_level(5), 1449)
        self.assertEqual(int(balancing.get_level_progress(1448) * 100), 99)

    def test_reward_of_a_new_assignment_grows_with_the_level(self):
        self.assertEqual(balancing.get_initial_assignment_reward(1), 50)
        self.assertEqual(balancing.get_initial_assignment_reward(4), 131)

    def test_reward_of_an_activity_follows_the_ratings(self):
        self.assertEqual(balancing.get_activity_reward(181, 2, 0), 181)
        self.assertEqual(balancing.get_activity_reward(181, 0, 2), 0)
        # Nobody rated: half of the reward, rounded up
        self.assertEqual(balancing.get_activity_reward(51, 0, 0), 26)
        self.assertEqual(balancing.get_activity_reward(40, 5, 1), 34)
        self.assertEqual(balancing.get_owner_reward(181), 18)


class SanitizerTest(SimpleTestCase):

    def test_text_becomes_one_capitalized_line(self):
        self.assertEqual(sanitizer.assignment_title("  juggle \n three   balls "), "Juggle three balls")

    def test_links_are_not_capitalized(self):
        self.assertEqual(sanitizer.activity_text("http://example.com/a"), "http://example.com/a")

    def test_length_limits(self):
        self.assertIsNone(sanitizer.assignment_title("ab"))
        self.assertIsNone(sanitizer.assignment_title("a" * 41))
        self.assertIsNone(sanitizer.activity_text("a" * 501))
        self.assertEqual(sanitizer.activity_text(""), "")
        self.assertIsNone(sanitizer.assignment_title(None))

    def test_person_name(self):
        self.assertEqual(sanitizer.person_name("  Jo  "), "Jo")
        self.assertIsNone(sanitizer.person_name("J"))
        self.assertIsNone(sanitizer.person_name("J" * 49))


class LinkifyTest(SimpleTestCase):

    def test_markup_in_user_text_is_escaped(self):
        self.assertEqual(linkify("<script>alert(1)</script> & more"),
                         "&lt;script&gt;alert(1)&lt;/script&gt; &amp; more")

    def test_urls_become_links_that_open_in_a_new_tab(self):
        self.assertEqual(
            linkify("see http://example.com/a?b=1&c=2."),
            'see <a href="http://example.com/a?b=1&amp;c=2" target="_blank" rel="noopener noreferrer">'
            'http://example.com/a?b=1&amp;c=2</a>.')
        self.assertIn('href="http://www.example.com"', linkify("www.example.com"))

    def test_a_url_cannot_break_out_of_the_link(self):
        html = linkify('http://example.com/"onmouseover="alert(1)')
        self.assertNotIn('"onmouseover', html)
        self.assertIn("&quot;onmouseover", html)

    def test_other_schemes_are_not_linked(self):
        self.assertNotIn("<a", linkify("javascript:alert(1) file:///etc/passwd"))


class SearchTest(TestCase):

    def setUp(self):
        search.index.clear()
        self.alice = helpers.member("Alice Wonder")
        self.juggling = helpers.skill(self.alice, "Juggling")
        self.assignment = helpers.assignment(self.alice, self.juggling, "Juggle three balls")

    def kinds(self, text):
        results, has_more = search.search(text, ANONYMOUS)
        return [(kind, item.get("title") or item.get("name")) for kind, item in results]

    def test_words_split_at_everything_that_is_not_a_letter(self):
        self.assertEqual(search.words("Type with-out 2 looking!"), ["type", "with", "out", "looking"])

    def test_finds_persons_categories_and_assignments_by_a_part_of_a_word(self):
        self.assertEqual(self.kinds("ugg"), [("skill", "Juggling"), ("assignment", "Juggle three balls")])
        self.assertEqual(self.kinds("WOND"), [("person", "Alice Wonder")])

    def test_every_word_of_the_query_has_to_match(self):
        self.assertEqual(self.kinds("three juggle"), [("assignment", "Juggle three balls")])
        self.assertEqual(self.kinds("three knives"), [])

    def test_quotes_are_ignored_and_long_words_never_match(self):
        self.assertEqual(self.kinds('"alice\''), [("person", "Alice Wonder")])
        self.assertEqual(self.kinds("a" * 16), [])

    def test_follows_changes(self):
        helpers.assignment(self.alice, self.juggling, "Juggle knives")
        self.assertIn(("assignment", "Juggle knives"), self.kinds("knives"))
        self.assignment.deleted = True
        self.assignment.save()
        self.assertEqual(self.kinds("three"), [])
        self.alice.name = "Alice Springs"
        self.alice.save()
        self.assertEqual(self.kinds("springs"), [("person", "Alice Springs")])
        self.assertEqual(self.kinds("wonder"), [])

    def test_at_most_five_results_and_a_hint_that_there_are_more(self):
        for number in "abcdefg":
            helpers.assignment(self.alice, self.juggling, "Juggle " + number * 3)
        results, has_more = search.search("juggle", ANONYMOUS)
        self.assertEqual(len(results), 5)
        self.assertTrue(has_more)

    def test_exact_words_come_before_longer_ones(self):
        helpers.skill(self.alice, "Jug")
        self.assertEqual(search.suggest_skills("jug", ANONYMOUS)[0]["title"], "Jug")

    def test_the_system_user_is_not_found(self):
        from mastery.models import Person
        Person.objects.create(id="00000000-0000-0000-0000-000000000000", name="System", picture=helpers.resource())
        search.index.clear()
        self.assertEqual(self.kinds("system"), [])
