/*
 * The client of Busy Humans.
 *
 * The server renders every page as a piece of HTML. This script puts the page for
 * the part of the address after the # into the frame, loads the lists page by page
 * while the user scrolls, and sends what the user does to the server.
 */
(function () {
    'use strict';

    var PAGE_SIZE = 10;
    var SYNC_INTERVAL = 600000;

    var body = document.body;
    var page = document.getElementById('page');
    var csrfToken = document.querySelector('meta[name="csrf-token"]').content;
    var loggedIn = body.dataset.status === 'AUTHORIZED' || body.dataset.status === 'NEEDS_TOS_CONFIRMATION';
    var loginEnabled = body.dataset.login === 'true';

    // What the user chose last, kept while moving between pages
    var sortBy = 'ACTIVITY';
    var personTab = 'PARTICIPATIONS';

    var navigation = 0;

    //
    // server
    //

    function get(url) {
        return fetch(url, {credentials: 'same-origin', headers: {'X-Requested-With': 'fetch'}});
    }

    /** Sends an action to the server. Resolves with its answer, or null if it failed. */
    function post(name, data) {
        return fetch('/action/' + name, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json', 'X-CSRFToken': csrfToken},
            body: JSON.stringify(data || {})
        }).then(function (response) {
            return response.json().catch(function () { return null; }).then(function (result) {
                if (!response.ok && !(result && result.error)) {
                    return {error: 'An error occured. Please try again later.'};
                }
                return result;
            });
        }).catch(function () {
            return {error: 'An error occured. Please try again later.'};
        });
    }

    /** Runs an action and shows the changed page. */
    function act(name, data) {
        return post(name, data).then(function (result) {
            if (result && result.error) {
                window.alert(result.error);
            }
            refresh();
            return result;
        });
    }

    //
    // navigation
    //

    function currentToken() {
        return decodeURIComponent(window.location.hash.replace(/^#/, ''));
    }

    function pageUrl(token) {
        var url = '/page?token=' + encodeURIComponent(token);
        if (token.indexOf('person=') === 0) {
            url += '&tab=' + personTab;
        } else {
            url += '&sort=' + sortBy;
        }
        return url;
    }

    function show(token, keepScroll) {
        var mine = ++navigation;
        var scroll = window.scrollY;
        return get(pageUrl(token)).then(function (response) {
            return response.text().then(function (html) {
                if (mine !== navigation) {
                    return;
                }
                page.innerHTML = html;
                setMenu(response.headers.get('X-Menu'));
                initPage();
                window.scrollTo(0, keepScroll ? scroll : 0);
            });
        });
    }

    function navigate() {
        closeLoginMenu();
        show(currentToken(), false);
    }

    /** Loads the current page again, for example after it changed. */
    function refresh() {
        refreshSidebar();
        return show(currentToken(), true);
    }

    function goTo(token) {
        if (currentToken() === token) {
            navigate();
        } else {
            window.location.hash = token;
        }
    }

    function setMenu(menu) {
        document.querySelectorAll('[data-menu]').forEach(function (anchor) {
            anchor.classList.toggle('topbar-anchor-active', anchor.dataset.menu === menu);
        });
    }

    function refreshSidebar() {
        get('/sidebar').then(function (response) { return response.text(); }).then(function (html) {
            document.getElementById('sidebar').innerHTML = html;
        });
    }

    //
    // lists that load more entries when their end comes into view
    //

    function isScrolledIntoView(element) {
        var rect = element.getBoundingClientRect();
        return rect.bottom <= window.innerHeight && rect.top >= 0;
    }

    function LazyList(element) {
        this.element = element;
        this.container = element.querySelector('.lazy-container');
        this.bottom = element.querySelector('.lazy-bottom');
        this.loading = element.querySelector('.lazy-loading');
        this.empty = element.querySelector('.lazy-empty');
        this.loaded = 0;
        this.shown = {};
        this.couldHaveMore = true;
        this.busy = false;
        element.lazyList = this;
        this.load(PAGE_SIZE);
    }

    LazyList.prototype.load = function (count) {
        var list = this;
        if (list.busy || !list.couldHaveMore || !document.body.contains(list.element)) {
            return;
        }
        list.busy = true;
        list.loading.style.display = '';
        var url = list.element.dataset.url;
        get(url + (url.indexOf('?') < 0 ? '?' : '&') + 'offset=' + list.loaded).then(function (response) {
            var covered = parseInt(response.headers.get('X-Count') || '0', 10);
            return response.text().then(function (html) {
                list.busy = false;
                list.loading.style.display = 'none';
                if (covered === 0) {
                    list.couldHaveMore = false;
                    list.updateEmpty();
                    return;
                }
                list.loaded += covered;
                var added = list.add(html);
                var remaining = count - added;
                if (remaining > 0) {
                    list.load(remaining);
                } else if (isScrolledIntoView(list.bottom)) {
                    list.load(PAGE_SIZE);
                }
            });
        }).catch(function () {
            list.busy = false;
            list.loading.style.display = 'none';
        });
    };

    LazyList.prototype.add = function (html) {
        var holder = document.createElement('div');
        holder.innerHTML = html;
        var added = 0;
        while (holder.firstElementChild) {
            var item = holder.firstElementChild;
            holder.removeChild(item);
            var id = item.dataset.id;
            if (!this.shown[id]) {
                this.shown[id] = true;
                this.container.appendChild(item);
                added++;
            }
        }
        this.updateEmpty();
        return added;
    };

    LazyList.prototype.updateEmpty = function () {
        var any = this.container.childElementCount > 0;
        this.container.style.display = any ? '' : 'none';
        this.empty.style.display = any || this.couldHaveMore ? 'none' : '';
    };

    window.addEventListener('scroll', function () {
        document.querySelectorAll('.lazy-list').forEach(function (element) {
            var list = element.lazyList;
            if (list && isScrolledIntoView(list.bottom)) {
                list.load(PAGE_SIZE);
            }
        });
    });

    //
    // pages
    //

    function initPage() {
        page.querySelectorAll('.lazy-list').forEach(function (element) {
            new LazyList(element);
        });
        if (page.querySelector('#new-assignment')) {
            initNewAssignment();
        }
        var tokenPage = page.querySelector('[data-token-action]');
        if (tokenPage && tokenPage.dataset.tokenAction === 'auto') {
            runToken(tokenPage);
        }
        var focus = page.querySelector('[data-autofocus]');
        if (focus) {
            focus.focus();
        }
    }

    function closest(element, selector) {
        return element && element.closest ? element.closest(selector) : null;
    }

    function assignmentId(element) {
        var holder = closest(element, '[data-assignment]');
        return holder ? holder.dataset.assignment : null;
    }

    function setDown(button, down) {
        button.classList.toggle('toggleButton-down', down);
        button.classList.toggle('toggleButton-up', !down);
        button.setAttribute('aria-pressed', down ? 'true' : 'false');
    }

    function isDown(button) {
        return button.classList.contains('toggleButton-down');
    }

    /** The buttons that need a login tell so and stay as they are. */
    function requireLogin() {
        if (!loggedIn) {
            window.alert('Please login.');
            return false;
        }
        return true;
    }

    var toggles = {
        'like': function (button) {
            var activity = closest(button, '[data-activity]').dataset.activity;
            act('rate', {activity: activity, value: isDown(button) ? 0 : 1});
        },
        'dislike': function (button) {
            var activity = closest(button, '[data-activity]').dataset.activity;
            act('rate', {activity: activity, value: isDown(button) ? 0 : -1});
        },
        'activity-abuse': function (button) {
            var activity = closest(button, '[data-activity]').dataset.activity;
            act('activity-abuse', {activity: activity, abuse: !isDown(button)});
        },
        'boost-up': function (button) {
            act('boost', {assignment: assignmentId(button), value: isDown(button) ? 0 : 1});
        },
        'boost-down': function (button) {
            act('boost', {assignment: assignmentId(button), value: isDown(button) ? 0 : -1});
        },
        'assignment-abuse': function (button) {
            act('assignment-abuse', {assignment: assignmentId(button), abuse: !isDown(button)});
        },
        'subscribe': function (button) {
            act('subscribe', {assignment: assignmentId(button), subscribed: !isDown(button)});
        },
        'skill-abuse': function (button) {
            var skill = closest(button, '[data-skill]').dataset.skill;
            act('skill-abuse', {skill: skill, abuse: !isDown(button)});
        }
    };

    var actions = {
        'delete-activity': function (button) {
            if (window.confirm('Delete activity?')) {
                act('delete-activity', {activity: closest(button, '[data-activity]').dataset.activity});
            }
        },
        'clear-activity-abuse': function (button) {
            act('clear-activity-abuse', {activity: closest(button, '[data-activity]').dataset.activity});
        },
        'delete-assignment': function (button) {
            var holder = closest(button, '[data-assignment]');
            if (window.confirm("Delete assignment '" + holder.dataset.title + "'?")) {
                post('delete-assignment', {assignment: holder.dataset.assignment}).then(function (result) {
                    if (result && result.ok) {
                        refreshSidebar();
                        goTo('discover');
                    } else {
                        refresh();
                    }
                });
            }
        },
        'clear-assignment-abuse': function (button) {
            act('clear-assignment-abuse', {assignment: assignmentId(button)});
        },
        'speedup': function (button) {
            act('speedup', {assignment: assignmentId(button), days: 1});
        },
        'slowdown': function (button) {
            act('speedup', {assignment: assignmentId(button), days: -1});
        },
        'delete-skill': function (button) {
            var holder = closest(button, '[data-skill]');
            if (window.confirm("Delete category '" + holder.dataset.title + "'? All Skills, Activities and Assignments"
                    + ' that use this category will be lost! This will be applied to all persons!')) {
                act('delete-skill', {skill: holder.dataset.skill});
            }
        },
        'clear-skill-abuse': function (button) {
            act('clear-skill-abuse', {skill: closest(button, '[data-skill]').dataset.skill});
        },
        'expand': function (button) {
            var achievement = closest(button, '.achievement');
            var collapsed = achievement.classList.toggle('collapsed');
            button.textContent = collapsed ? 'v' : '^';
        },
        'post-activity': postActivity,
        'remove-block': function (button) {
            var block = closest(button, '[data-block]');
            block.parentNode.removeChild(block);
        },
        'agree': function () {
            post('confirm-tos').then(function () { window.location.reload(); });
        },
        'register': register,
        'save-preferences': savePreferences,
        'request-reset': requestReset,
        'run-token': function (button) { runToken(closest(button, '[data-token-action]')); },
        'create-assignment': createAssignment,
        'new-skill': newSkill,
        'change-skill': changeSkill,
        'select-skill': function (button) { selectSkill(closest(button, '[data-skill-id]')); },
        'subscribe-all': function () { post('subscribe-all').then(alertError); },
        'send-notifications': function () { post('send-notifications').then(alertError); },
        'create-announcement': createAnnouncement,
        'create-test-person': createTestPerson
    };

    function alertError(result) {
        if (result && result.error) {
            window.alert(result.error);
        }
        return result;
    }

    document.addEventListener('click', function (event) {
        var toggle = closest(event.target, '[data-toggle]');
        if (toggle) {
            if (requireLogin()) {
                toggles[toggle.dataset.toggle](toggle);
            }
            return;
        }
        var tab = closest(event.target, '[data-tab]');
        if (tab) {
            personTab = tab.dataset.tab;
            show(currentToken(), true);
            return;
        }
        var action = closest(event.target, '[data-action]');
        if (action && actions[action.dataset.action]) {
            event.preventDefault();
            actions[action.dataset.action](action);
            return;
        }
        var license = closest(event.target, '.PictureV-licenseButton');
        if (license) {
            event.preventDefault();
            showLicense(closest(license, '.picture'));
            return;
        }
        // A click on the entry of the current page loads it again
        var anchor = closest(event.target, 'a[href^="#"]');
        if (anchor && anchor.getAttribute('href').substring(1) === currentToken()) {
            navigate();
        }
    });

    document.addEventListener('change', function (event) {
        if (event.target.name === 'sortBy') {
            sortBy = event.target.value;
            show(currentToken(), true);
        } else if (event.target.id === 'cloaks') {
            post('set-cloak', {person: event.target.value}).then(function () { window.location.reload(); });
        } else if (event.target.id === 'change-email') {
            page.querySelectorAll('.email-field').forEach(function (field) {
                field.disabled = !event.target.checked;
            });
        } else if (event.target.matches('.upload input[type="file"]')) {
            upload(event.target);
        } else if (event.target.id === 'activity-text') {
            activityTextChanged(event.target);
        }
    });

    //
    // pictures
    //

    function showLicense(picture) {
        closeLicense();
        var popup = document.createElement('div');
        popup.className = 'license-popup';
        var outer = document.createElement('div');
        outer.className = 'LicenseV-outer';
        outer.tabIndex = 0;
        var inner = document.createElement('div');
        function part(label, text, href) {
            var span = document.createElement('span');
            span.className = 'LicenseV-nowrap';
            span.textContent = label;
            var anchor = document.createElement('a');
            anchor.className = 'LicenseV-nowrap';
            anchor.textContent = text;
            // Only real links: the author url comes from other users
            if (/^(https?:\/\/|#)/.test(href)) {
                anchor.href = href;
                anchor.target = '_blank';
                anchor.rel = 'noopener noreferrer';
            }
            inner.appendChild(span);
            inner.appendChild(anchor);
            inner.appendChild(document.createTextNode(' '));
        }
        part('Picture by: ', picture.dataset.authorName, picture.dataset.authorUrl);
        part('License: ', picture.dataset.licenseName, picture.dataset.licenseUrl);
        outer.appendChild(inner);
        popup.appendChild(outer);
        var rect = picture.getBoundingClientRect();
        popup.style.left = (rect.left + window.scrollX) + 'px';
        popup.style.top = (rect.top + window.scrollY) + 'px';
        outer.addEventListener('mouseleave', closeLicense);
        document.body.appendChild(popup);
    }

    function closeLicense() {
        document.querySelectorAll('.license-popup').forEach(function (popup) {
            popup.parentNode.removeChild(popup);
        });
    }

    document.addEventListener('mousedown', function (event) {
        if (!closest(event.target, '.license-popup')) {
            closeLicense();
        }
    });

    //
    // uploads
    //

    function upload(input) {
        var holder = closest(input, '.upload');
        var status = holder.querySelector('.upload-status');
        var file = input.files[0];
        if (!file) {
            return;
        }
        var data = new FormData();
        data.append('file', file);
        if (holder.dataset.upload === 'register') {
            data.append('register', 'true');
        }
        status.textContent = 'Uploading...';
        fetch('/mastery/upload', {
            method: 'POST', credentials: 'same-origin', headers: {'X-CSRFToken': csrfToken}, body: data
        }).then(function (response) {
            return response.json().catch(function () { return {error: 'The upload failed.'}; });
        }).catch(function () {
            return {error: 'The upload failed.'};
        }).then(function (result) {
            input.value = '';
            if (result.error) {
                status.textContent = result.error;
                return;
            }
            status.textContent = '';
            uploaded(holder, result);
        });
    }

    function uploaded(holder, result) {
        var kind = holder.dataset.upload;
        if (kind === 'activity') {
            addBlock(result.html);
        } else {
            // One picture: for the registration, a new category or a test person
            var target = page.querySelector(holder.dataset.target);
            target.dataset.token = result.id;
            target.querySelectorAll('img').forEach(function (image) { image.src = result.medium; });
            if (kind === 'skill') {
                updateSkillPicture();
            }
        }
    }

    //
    // new activity
    //

    var YOUTUBE = /(^|\s)((http(s)?:\/\/)?(www\.)?youtu(be.\w+|.be)\/(watch\?v=|embed\/)?(([\w-])+)([&#][&#=\-%\w]*)*)($|\s|\.)/gm;

    function addBlock(html) {
        var holder = document.createElement('div');
        holder.innerHTML = html;
        var blocks = page.querySelector('#activity-blocks');
        while (holder.firstElementChild) {
            blocks.appendChild(holder.firstElementChild);
        }
    }

    function sanitizeText(text, maxLength) {
        if (text.length > maxLength) {
            return null;
        }
        text = text.replace(/\r/g, '').replace(/\n/g, ' ').trim().replace(/\s+/g, ' ');
        return capitalize(text);
    }

    function capitalize(text) {
        if (text.indexOf('http') === 0) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /** Links to YouTube videos in the text become videos of the activity. */
    function activityTextChanged(textarea) {
        var text = textarea.value;
        var result = text;
        var match;
        var videos = [];
        YOUTUBE.lastIndex = 0;
        while ((match = YOUTUBE.exec(text)) !== null) {
            videos.push(match[8]);
            result = result.replace(match[2], '');
        }
        videos.forEach(function (video) {
            post('video-block', {video: video}).then(function (answer) {
                if (answer && answer.html) {
                    addBlock(answer.html);
                }
            });
        });
        var clean = sanitizeText(result, 500);
        textarea.value = clean === null ? '' : clean;
    }

    function postActivity(button) {
        var form = closest(button, '#new-activity');
        var textarea = form.querySelector('#activity-text');
        var text = sanitizeText(textarea.value, 500);
        var blocks = [];
        form.querySelectorAll('[data-block]').forEach(function (block) {
            blocks.push({typ: parseInt(block.dataset.block, 10), value: block.dataset.value});
        });
        if (blocks.length === 0 && !text) {
            window.alert('Please describe your activity with text, pictures or videos.');
            return;
        }
        button.disabled = true;
        textarea.disabled = true;
        act('create-activity', {assignment: form.dataset.assignment, text: text || '', blocks: blocks});
    }

    document.addEventListener('keydown', function (event) {
        // These texts are one line
        if (event.key === 'Enter' && event.target.matches('#activity-text, #assignment-description, #skill-description')) {
            event.preventDefault();
        }
    });

    document.addEventListener('keyup', function (event) {
        var target = event.target;
        if (target.matches('#activity-text, #assignment-description, #skill-description') && target.value.length > 500) {
            target.value = target.value.substring(0, 500);
        }
        if (target.id === 'search') {
            searchChanged();
        } else if (target.id === 'assignment-title') {
            titleChanged();
        } else if (target.id === 'assignment-description') {
            descriptionChanged(false);
        } else if (target.id === 'skill-search') {
            later('skill', skillSearchChanged);
        } else if (event.key === 'Enter') {
            if (target.id === 'login-email' || target.id === 'login-password') {
                login();
            } else if (target.matches('.email-field')) {
                savePreferences();
            }
        }
    });

    var timers = {};

    function later(name, action) {
        window.clearTimeout(timers[name]);
        timers[name] = window.setTimeout(action, 250);
    }

    //
    // new assignment
    //

    var newAssignment = null;

    function initNewAssignment() {
        newAssignment = {exists: false, skillValid: false, skillId: null, newSkill: false, wikipedia: false};
        titleChanged();
        descriptionChanged(false);
        setSkillValid(false);
        skillSearchChanged();
    }

    function validation(id, text) {
        var label = page.querySelector(id);
        label.textContent = text || '';
        label.style.display = text ? '' : 'none';
    }

    function updateCreateButton() {
        var invalid = newAssignment.exists
            || page.querySelector('#title-validation').style.display !== 'none'
            || page.querySelector('#description-validation').style.display !== 'none'
            || !newAssignment.skillValid;
        page.querySelector('#assignment-create').disabled = invalid;
    }

    function titleChanged() {
        var input = page.querySelector('#assignment-title');
        newAssignment.exists = false;
        var clean = input.value.length >= 3 ? sanitizeText(input.value, 40) : null;
        if (clean === null) {
            validation('#title-validation', 'Please enter a short title for the assignment.');
        } else {
            validation('#title-validation', '');
            later('title', function () {
                post('assignment-exists', {title: clean}).then(function (result) {
                    if (!newAssignment || !document.body.contains(input)) {
                        return;
                    }
                    newAssignment.exists = !!(result && result.exists);
                    validation('#title-validation', newAssignment.exists ? 'An assigment with this title already exists.' : '');
                    updateCreateButton();
                });
            });
        }
        updateCreateButton();
    }

    function descriptionChanged(capitalizeIt) {
        var input = page.querySelector('#assignment-description');
        var text = input.value.replace(/\r/g, '').replace(/\n/g, ' ');
        if (capitalizeIt) {
            text = capitalize(text);
        }
        if (text !== input.value) {
            input.value = text;
        }
        validation('#description-validation', sanitizeText(text, 500) === null
            ? 'Please describe the assignment with up to 500 characters.' : '');
        updateCreateButton();
    }

    document.addEventListener('focusout', function (event) {
        if (!newAssignment) {
            return;
        }
        if (event.target.id === 'assignment-title') {
            event.target.value = capitalize(event.target.value);
            titleChanged();
        } else if (event.target.id === 'assignment-description') {
            descriptionChanged(true);
        } else if (event.target.id === 'skill-description') {
            event.target.value = capitalize(event.target.value.replace(/\r/g, '').replace(/\n/g, ' '));
            newAssignment.wikipedia = false;
        }
    });

    function setSkillValid(valid) {
        newAssignment.skillValid = valid;
        validation('#skill-validation', valid ? '' : 'Please select or create a category.');
        if (!valid) {
            page.querySelector('#assignment-picture img').src = 'static/default-skill.png';
        }
        updateCreateButton();
    }

    function skillSearchChanged() {
        var input = page.querySelector('#skill-search');
        if (!input) {
            return;
        }
        var existing = page.querySelector('#existing-skills');
        existing.innerHTML = '';
        var clean = input.value.length >= 3 ? sanitizeText(input.value, 40) : null;
        var enterTerm = page.querySelector('#skill-enter-term');
        var notFound = page.querySelector('#skill-not-found');
        var loading = page.querySelector('#skill-loading');
        if (clean === null) {
            loading.style.display = 'none';
            notFound.style.display = 'none';
            enterTerm.style.display = '';
            return;
        }
        enterTerm.style.display = 'none';
        notFound.style.display = 'none';
        loading.style.display = '';
        post('suggest-skills', {title: clean}).then(function (result) {
            if (!document.body.contains(input)) {
                return;
            }
            loading.style.display = 'none';
            existing.innerHTML = (result && result.html) || '';
            notFound.style.display = result && result.exact ? 'none' : '';
        });
    }

    function showSelectedSkill(html) {
        page.querySelector('#selected-skill').innerHTML = html;
        page.querySelector('#skill-search-panel').style.display = 'none';
        page.querySelector('#skill-selected-panel').style.display = '';
    }

    function selectSkill(element) {
        newAssignment.skillId = element.dataset.skillId;
        newAssignment.newSkill = false;
        var copy = element.cloneNode(true);
        copy.classList.remove('selectable');
        copy.removeAttribute('data-action');
        showSelectedSkill(copy.outerHTML);
        page.querySelector('#assignment-picture img').src = element.dataset.picture;
        setSkillValid(true);
    }

    function changeSkill() {
        newAssignment.skillId = null;
        newAssignment.newSkill = false;
        page.querySelector('#selected-skill').innerHTML = '';
        page.querySelector('#skill-selected-panel').style.display = 'none';
        page.querySelector('#skill-search-panel').style.display = '';
        setSkillValid(false);
    }

    function newSkill() {
        var title = sanitizeText(page.querySelector('#skill-search').value, 40);
        if (!title) {
            return;
        }
        newAssignment.skillId = null;
        newAssignment.newSkill = true;
        newAssignment.wikipedia = false;
        showSelectedSkill(page.querySelector('#new-skill-template').innerHTML);
        page.querySelector('#new-skill-title').textContent = title;
        updateSkillPicture();
        var description = page.querySelector('#skill-description');
        description.value = 'Searching Wikipedia...';
        description.disabled = true;
        wikipediaSummary(title).then(function (summary) {
            if (!document.body.contains(description)) {
                return;
            }
            var first = summary.length > 50 ? firstSentences(summary) : null;
            description.value = first || '';
            newAssignment.wikipedia = !!first;
            description.disabled = false;
        });
    }

    /** A new category is complete as soon as it has a picture, and it always has one. */
    function updateSkillPicture() {
        var picture = page.querySelector('#new-skill-picture img');
        if (picture) {
            page.querySelector('#assignment-picture img').src = picture.getAttribute('src');
            setSkillValid(true);
        }
    }

    function wikipediaSummary(title) {
        var url = 'https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&redirects=1'
            + '&format=json&origin=*&titles=' + encodeURIComponent(title);
        return fetch(url).then(function (response) { return response.json(); }).then(function (data) {
            var pages = (data.query && data.query.pages) || {};
            for (var key in pages) {
                if (pages[key].extract) {
                    return pages[key].extract.replace(/\[\d+\]/g, '');
                }
            }
            return '';
        }).catch(function () { return ''; });
    }

    function firstSentences(summary) {
        var text = sanitizeText(summary.substring(0, 500), 500);
        if (text === null) {
            return null;
        }
        var period = -1;
        for (var i = 0; i < 3; i++) {
            var next = text.indexOf('.', period + 1);
            if (next === -1) {
                return text;
            }
            period = next;
        }
        return text.substring(0, period + 1);
    }

    function createAssignment(button) {
        var data = {
            title: page.querySelector('#assignment-title').value,
            description: page.querySelector('#assignment-description').value,
            skill: newAssignment.skillId
        };
        if (newAssignment.newSkill) {
            data.newSkill = {
                title: page.querySelector('#new-skill-title').textContent,
                description: page.querySelector('#skill-description').value,
                wikipedia: newAssignment.wikipedia,
                picture: page.querySelector('#new-skill-picture').dataset.token || null
            };
        }
        button.disabled = true;
        button.textContent = 'Please wait...';
        post('create-assignment', data).then(function (result) {
            if (result && result.id) {
                refreshSidebar();
                window.location.hash = 'assignment=' + result.id;
            } else {
                window.alert((result && result.error) || 'An error occured. Please try again later.');
                button.disabled = false;
                button.textContent = 'Create';
            }
        });
    }

    //
    // login, registration, preferences
    //

    function byId(id) {
        return document.getElementById(id);
    }

    function closeLoginMenu() {
        var dropdown = byId('login-dropdown');
        if (!dropdown) {
            return;
        }
        dropdown.style.display = 'none';
        byId('login-selection').style.display = '';
        byId('login-form').style.display = 'none';
        byId('login-email').value = '';
        byId('login-password').value = '';
        var button = byId('login-menu-button');
        button.classList.remove('topbar-togglebutton-down');
        button.classList.add('topbar-togglebutton-up');
    }

    function login() {
        var email = byId('login-email').value;
        var password = byId('login-password').value;
        if (!email || !password) {
            return;
        }
        post('login', {email: email, password: password}).then(function (result) {
            if (result && result.ok) {
                window.location.reload();
            } else {
                window.alert((result && result.error) || 'Wrong username or password.');
                closeLoginMenu();
            }
        });
    }

    if (loginEnabled) {
        document.addEventListener('click', function (event) {
            var target = event.target;
            if (closest(target, '#login-menu-button')) {
                var dropdown = byId('login-dropdown');
                var open = dropdown.style.display === 'none';
                closeLoginMenu();
                if (open) {
                    dropdown.style.display = '';
                    byId('login-menu-button').classList.remove('topbar-togglebutton-up');
                    byId('login-menu-button').classList.add('topbar-togglebutton-down');
                }
            } else if (target.id === 'login-with-account') {
                byId('login-selection').style.display = 'none';
                byId('login-form').style.display = '';
                byId('login-email').focus();
            } else if (target.id === 'login-cancel') {
                byId('login-selection').style.display = '';
                byId('login-form').style.display = 'none';
            } else if (target.id === 'login-submit') {
                login();
            } else if (target.id === 'login-register') {
                closeLoginMenu();
                window.location.hash = 'register';
            } else if (closest(target, '#logout-button')) {
                post('logout').then(function () { window.location.reload(); });
            } else if (closest(target, '#preferences-button')) {
                goTo('preferences');
            }
        });
    }

    function value(id) {
        return page.querySelector(id).value;
    }

    function register() {
        var email = value('#register-email');
        var password = value('#register-password1');
        var name = value('#register-name');
        var error = function (id, text) { page.querySelector(id).textContent = text; return !!text; };

        if (error('#register-email-error', /^[^@]+@[^@]+\.[^@.]+$/.test(email) ? '' : 'The email address seems to be invalid.')) {
            return;
        }
        if (error('#register-password-error', password === value('#register-password2') ? '' : 'Both passwords must be equal.')) {
            return;
        }
        if (error('#register-password-error', password.length < 8 ? 'The password must consist of at least 8 characters.'
                : password.length > 64 ? 'The password must not consist of more than 64 characters.' : '')) {
            return;
        }
        if (error('#register-name-error', name.trim().length < 2 ? 'The name must consist of at least 2 characters.'
                : name.trim().length > 48 ? 'The name must not consist of more than 48 characters.' : '')) {
            return;
        }
        post('register', {
            email: email, password: password, name: name, website: value('#register-website'),
            picture: page.querySelector('#register-picture').dataset.token || null
        }).then(function (result) {
            if (result && result.error) {
                window.alert(result.error);
            } else if (result && result.message) {
                page.querySelector('#register-form').style.display = 'none';
                page.querySelector('#register-done').textContent = result.message;
            }
        });
    }

    function savePreferences() {
        var checked = function (id) { return page.querySelector(id).checked; };
        var data = {
            subscribeOwnAssignments: checked('#pref-subscribe'),
            notifyOtherActivity: checked('#pref-activity'),
            notifyActivityReward: checked('#pref-activity-reward'),
            notifyOtherActivityReward: checked('#pref-other-activity-reward')
        };
        var save = page.querySelector('#pref-save');
        save.disabled = true;
        post('preferences', data).then(function (result) {
            save.disabled = false;
            alertError(result);
        });
        if (checked('#change-email')) {
            var email = value('#pref-email');
            if (email !== value('#pref-email2')) {
                window.alert('Please enter the same email address in both fields.');
            } else if (!/^[^@]+@[^@]+\.[^@.]+$/.test(email)) {
                window.alert('Please enter a valid email address.');
            } else {
                post('change-email', {email: email, password: value('#pref-password')}).then(function (result) {
                    page.querySelector('#pref-password').value = '';
                    window.alert((result && (result.error || result.message)) || 'An error occured.');
                });
            }
        }
    }

    function requestReset() {
        post('request-reset', {email: value('#reset-email')}).then(function (result) {
            page.querySelector('#token-message').textContent = (result && (result.error || result.message)) || '';
        });
    }

    /** Pages that are opened from a link in an email: confirm an address or set a new password. */
    function runToken(holder) {
        var data = {token: holder.dataset.token};
        var password = holder.querySelector('#reset-password1');
        if (password) {
            if (password.value !== holder.querySelector('#reset-password2').value) {
                window.alert('Both passwords must be equal.');
                return;
            }
            data.password = password.value;
        }
        post(holder.dataset.tokenName, data).then(function (result) {
            if (result && result.ok) {
                window.location.hash = 'discover';
                window.location.reload();
            } else {
                page.querySelector('#token-message').textContent =
                    (result && result.error) || 'This link is not valid any more.';
            }
        });
    }

    //
    // admin
    //

    function createAnnouncement() {
        var text = value('#announcement-text');
        var show = parseInt(value('#announcement-show'), 10);
        var hideText = value('#announcement-hide');
        var hide = hideText === '' ? null : parseInt(hideText, 10);
        if (isNaN(show) || (hide !== null && isNaN(hide))) {
            window.alert('Show/hide time must be a number.');
            return;
        }
        if (!text || show < 0 || (hide !== null && hide <= 0)) {
            window.alert('Missing text or due time.');
            return;
        }
        post('create-announcement', {
            text: text, show: show, hide: hide, maintenance: page.querySelector('#announcement-maintenance').checked
        }).then(function (result) {
            alertError(result);
            refresh();
            sync();
        });
    }

    function createTestPerson() {
        var name = value('#test-person-name');
        var picture = page.querySelector('#test-person-picture').dataset.token;
        if (!name || !picture) {
            window.alert('Missing name or picture.');
            return;
        }
        post('create-test-person', {name: name, picture: picture}).then(function (result) {
            alertError(result);
            refresh();
        });
    }

    //
    // search
    //

    function searchChanged() {
        later('search', function () {
            var input = byId('search');
            var results = byId('search-results');
            var more = byId('search-more');
            var text = input.value.replace(/"/g, '').replace(/'/g, '').trim();
            results.innerHTML = '';
            more.style.display = 'none';
            if (!text) {
                return;
            }
            get('/search?q=' + encodeURIComponent(text)).then(function (response) {
                return response.text().then(function (html) {
                    if (input.value.replace(/"/g, '').replace(/'/g, '').trim() !== text) {
                        return;
                    }
                    results.innerHTML = html;
                    more.style.display = response.headers.get('X-More') === 'true' ? '' : 'none';
                });
            });
        });
    }

    //
    // announcements
    //

    var clockOffset = 0;
    var knownAnnouncements = {};
    var announcementQueue = [];
    var shownAnnouncement = null;

    function serverTime() {
        return Date.now() + clockOffset;
    }

    function natural(deltaMs) {
        var second = 1000, minute = 60 * second, hour = 60 * minute, day = 24 * hour;
        var week = 7 * day, month = 30 * day, year = 365 * day;
        var steps = [[2 * minute, second, ' seconds'], [2 * hour, minute, ' minutes'], [2 * day, hour, ' hours'],
            [2 * week, day, ' days'], [2 * month, week, ' weeks'], [2 * year, month, ' months']];
        for (var i = 0; i < steps.length; i++) {
            if (deltaMs < steps[i][0]) {
                return Math.round(deltaMs / steps[i][1]) + steps[i][2];
            }
        }
        return Math.round(deltaMs / year) + ' years';
    }

    function renderAnnouncement() {
        var holder = byId('announcement');
        var data = shownAnnouncement;
        if (!data) {
            return;
        }
        var text = data.text.replace('{show}', natural(serverTime() - data.showTime) + ' ago');
        if (data.hideTime !== null) {
            text = text.replace('{hide}', serverTime() > data.hideTime ? 'now' : 'in ' + natural(data.hideTime - serverTime()));
        }
        holder.querySelector('.AnnouncementV-text').textContent = text;
        var closable = !data.maintenance || data.hideTime === null || serverTime() > data.hideTime;
        holder.querySelector('.AnnouncementV-close').style.display = closable ? '' : 'none';
    }

    function showNextAnnouncement() {
        var holder = byId('announcement');
        holder.innerHTML = '';
        shownAnnouncement = announcementQueue.shift() || null;
        byId('announcements').style.display = shownAnnouncement ? '' : 'none';
        if (!shownAnnouncement) {
            return;
        }
        var text = document.createElement('div');
        text.className = 'AnnouncementV-text';
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'button AnnouncementV-close';
        close.textContent = 'OK';
        close.addEventListener('click', function () {
            var closed = shownAnnouncement;
            showNextAnnouncement();
            if (loggedIn) {
                post('announcement-closed', {showTime: closed.showTime});
            }
        });
        var wrapper = document.createElement('div');
        wrapper.appendChild(text);
        wrapper.appendChild(close);
        holder.appendChild(wrapper);
        renderAnnouncement();
    }

    function sync() {
        var sent = Date.now();
        get('/sync').then(function (response) { return response.json(); }).then(function (result) {
            clockOffset = result.serverTime - Date.now() + (Date.now() - sent) / 2;
            result.announcements.forEach(function (announcement) {
                if (!knownAnnouncements[announcement.id]) {
                    knownAnnouncements[announcement.id] = true;
                    announcementQueue.push(announcement);
                }
            });
            if (!shownAnnouncement) {
                showNextAnnouncement();
            }
        }).catch(function () {});
    }

    //
    // start
    //

    byId('logo').addEventListener('click', function () {
        window.location.assign(window.location.href.split('#')[0]);
    });
    window.addEventListener('hashchange', navigate);
    window.setInterval(renderAnnouncement, 1000);
    window.setInterval(sync, SYNC_INTERVAL);
    sync();
    navigate();
})();
