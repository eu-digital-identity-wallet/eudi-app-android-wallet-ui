source "https://rubygems.org"

gem "fastlane"
gem "fastlane-plugin-run_tests_firebase_testlab"
gem "fastlane-plugin-commit_android_version_bump"
gem "fastlane-plugin-versioning_android"

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
