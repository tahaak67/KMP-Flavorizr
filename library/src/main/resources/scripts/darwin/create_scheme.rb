#!/usr/bin/env ruby
# Creates an Xcode scheme for a flavor.
# Usage: ruby create_scheme.rb <project_path> <flavor_name> <target_name>

require 'xcodeproj'

project_path = ARGV[0]
flavor_name = ARGV[1]
target_name = ARGV[2] || 'iosApp'

project = Xcodeproj::Project.open(project_path)

# Find the target by name
app_target = project.targets.find { |t| t.name == target_name }
unless app_target
  # Fall back to first native target
  app_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.application' }
end
unless app_target
  available = project.targets.map { |t| "'#{t.name}'" }.join(', ')
  puts "ERROR: Target '#{target_name}' not found. Available targets: #{available}"
  exit 1
end

puts "Using target: '#{app_target.name}'"

# Check if scheme already exists
schemes_dir = File.join(project_path, 'xcshareddata', 'xcschemes')
scheme_path = File.join(schemes_dir, "#{flavor_name}.xcscheme")

if File.exist?(scheme_path)
  puts "Scheme '#{flavor_name}' already exists, recreating"
  File.delete(scheme_path)
end

# Create scheme
scheme = Xcodeproj::XCScheme.new

# Build action
build_action = scheme.build_action
build_action.add_entry(
  Xcodeproj::XCScheme::BuildAction::Entry.new(app_target)
)

# Test action
scheme.test_action.build_configuration = "Debug-#{flavor_name}"

# Launch action — include the app binary so IntelliJ/Xcode don't prompt for it
scheme.launch_action.build_configuration = "Debug-#{flavor_name}"
scheme.launch_action.buildable_product_runnable =
  Xcodeproj::XCScheme::BuildableProductRunnable.new(app_target, '0')

# Profile action
scheme.profile_action.build_configuration = "Profile-#{flavor_name}"
scheme.profile_action.buildable_product_runnable =
  Xcodeproj::XCScheme::BuildableProductRunnable.new(app_target, '0')

# Analyze action
scheme.analyze_action.build_configuration = "Debug-#{flavor_name}"

# Archive action
scheme.archive_action.build_configuration = "Release-#{flavor_name}"

# Save scheme
FileUtils.mkdir_p(schemes_dir)
scheme.save_as(project_path, flavor_name, true)

puts "Created scheme '#{flavor_name}'"
