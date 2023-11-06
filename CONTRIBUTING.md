# Contribution Guidelines

We welcome contributions to this project. To ensure that the process is smooth for everyone
involved, please follow the guidelines below.

If you encounter a bug in the project, check if the bug has already been reported. If the
bug has not been reported, you can open an issue to report the bug.

Before making any changes, it's a good practice to create an issue to describe the changes
you plan to make and the reasoning behind them.

You can
read [Finding ways to contribute to open source on GitHub](https://docs.github.com/en/get-started/exploring-projects-on-github/finding-ways-to-contribute-to-open-source-on-github)
for more information.

## GitHub Flow

We use the [GitHub Flow](https://guides.github.com/introduction/flow/) workflow for making
contributions to this project. This means that:

1. Fork the repository and create a new branch from `main` for your changes.

   ```bash
   git checkout main
   git pull
   git checkout -b my-branch
   ```

2. Make changes to the code, documentation, or any other relevant files.
3. Commit your changes and push them to your forked repository.

   ```bash
   git add .
   git commit -m "Add a new feature"
   git push origin my-branch
   ```

4. Create a pull request from your branch to the `main` branch of this repository.

## Pull Request Checklist

* Branch from the main branch and, if needed, rebase to the current main branch before submitting
  your pull request. If it doesn't merge cleanly with main you may be asked to rebase your changes.

* Commits should be as small as possible while ensuring that each commit is correct independently (
  i.e., each commit should compile and pass tests).

* Test your changes as thoroughly as possible before you commit them. Preferably, automate your test
  by unit/integration tests. If tested manually, provide information about the test scope in the PR
  description (e.g. “Test passed: Upgrade version from 0.42 to 0.42.23.”).

* Create _Work In Progress [WIP]_ pull requests only if you need clarification or an explicit review
  before you can continue your work item.

* If your patch is not getting reviewed or you need a specific person to review it, you can @-reply
  a reviewer asking for a review in the pull request or a comment.

* Post review:
    * If a review requires you to change your commit(s), please test the changes again.
    * Amend the affected commit(s) and force push onto your branch.
    * Set respective comments in your GitHub review to resolved.
    * Create a general PR comment to notify the reviewers that your amendments are ready for another
      round of review.

## Branch Name Rules

Please name your branch using the following convention:

```text
<type>/<short-description>
```

- `type` should be one of the following:
    - `feat` for a new feature,
    - `fix` for a bug fix,
    - `docs` for documentation changes,
    - `style` for changes that do not affect the code, such as formatting or whitespace,
    - `refactor` for code refactoring,
    - `test` for adding or updating tests, or
    - `chore` for any other miscellaneous tasks.
- `short-description` should be a short, descriptive name of the changes you are making.

For example:

```text
feat/add-new-button
fix/typo-in-readme
docs/update-contributing-guide
style/format-code
refactor/extract-method
test/add-unit-tests
chore/update-dependencies
```

## Issues and Planning

* We use GitHub issues to track bugs and enhancement requests.

* Please provide as much context as possible when you open an issue. The information you provide
  must be comprehensive enough to reproduce that issue for the assignee. Therefore, contributors may
  use but aren't restricted to the issue template provided by the project maintainers.

* When creating an issue, try using one of our issue templates which already contain some guidelines
  on which content is expected to process the issue most efficiently. If no template applies, you
  can of course also create an issue from scratch.

* Please apply one or more applicable [labels](/../../labels) to your issue so that all community
  members are able to cluster the issues better.

## Code of Conduct

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md).
By participating in this project, you agree to abide by its terms.