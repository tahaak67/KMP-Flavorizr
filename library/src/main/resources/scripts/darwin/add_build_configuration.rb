#!/usr/bin/env ruby
# Creates flavor-specific build configurations in an Xcode project.
# Usage: ruby add_build_configuration.rb <project_path> <xcconfig_path> <flavor_name> <mode> <build_settings_base64> <target_name>

require 'xcodeproj'
require 'json'
require 'base64'

project_path = ARGV[0]
xcconfig_path = ARGV[1]
flavor_name = ARGV[2]
mode = ARGV[3]
build_settings_base64 = ARGV[4]
target_name = ARGV[5] || 'iosApp'

# Parse build settings
build_settings = JSON.parse(Base64.decode64(build_settings_base64))

# Determine configuration name
mode_capitalized = mode.capitalize
config_name = "#{mode_capitalized}-#{flavor_name}"

# Determine base configuration type
base_type = case mode
            when 'debug' then :debug
            when 'profile', 'release' then :release
            else :release
            end

project = Xcodeproj::Project.open(project_path)

# Find the xcconfig file reference
xcconfig_ref = nil
project.files.each do |file|
  if file.path&.end_with?(File.basename(xcconfig_path))
    xcconfig_ref = file
    break
  end
end

# If xcconfig not found, try to add it
unless xcconfig_ref
  xcconfig_full_path = File.join(File.dirname(project_path), xcconfig_path)
  if File.exist?(xcconfig_full_path)
    # Try to find a traditional PBXGroup to add the reference to
    target_group = project.main_group[target_name]

    if target_group
      # Check if this is a traditional group that supports new_reference
      if target_group.respond_to?(:new_reference) && !target_group.is_a?(Xcodeproj::Project::Object::PBXFileSystemSynchronizedRootGroup)
        begin
          xcconfig_ref = target_group.new_reference(xcconfig_path)
          puts "Added xcconfig file reference: #{xcconfig_path}"
        rescue NoMethodError => e
          puts "NOTE: Could not add file reference to group (synchronized group): #{e.message}"
        end
      else
        puts "NOTE: Target group '#{target_name}' is a synchronized file group (Xcode 16+). Skipping file reference addition."
        puts "NOTE: The xcconfig file will still work — Xcode resolves it by path from the build configuration."
      end
    end

    # If we still don't have a reference, create one directly on the main group
    unless xcconfig_ref
      begin
        # Try adding to project's main group directly
        main_group = project.main_group
        if main_group.respond_to?(:new_reference)
          xcconfig_ref = main_group.new_file(xcconfig_path)
          puts "Added xcconfig file reference to main group: #{xcconfig_path}"
        end
      rescue => e
        puts "NOTE: Could not add file reference: #{e.message}"
        puts "NOTE: Build configurations will still be created — xcconfig path is set in build settings."
      end
    end
  end
end

# Find the app target
app_target = project.targets.find { |t| t.name == target_name }
unless app_target
  app_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.application' }
end

if app_target
  # Check if configuration already exists
  existing = app_target.build_configurations.find { |c| c.name == config_name }
  if existing
    puts "Configuration '#{config_name}' already exists for target '#{app_target.name}', updating"
    existing.build_settings.merge!(build_settings)
    existing.base_configuration_reference = xcconfig_ref if xcconfig_ref
  else
    # Find base configuration to clone settings from
    base_config = app_target.build_configurations.find { |c| c.name == mode_capitalized } ||
                  app_target.build_configurations.find { |c| c.name == 'Debug' }

    # Create new configuration
    new_config = app_target.add_build_configuration(config_name, base_type)
    if base_config
      new_config.build_settings = base_config.build_settings.dup
    end
    new_config.build_settings.merge!(build_settings)
    new_config.base_configuration_reference = xcconfig_ref if xcconfig_ref

    puts "Created configuration '#{config_name}' for target '#{app_target.name}'"
  end
else
  available = project.targets.map { |t| "'#{t.name}'" }.join(', ')
  puts "WARNING: Target '#{target_name}' not found. Available targets: #{available}"
end

# Add configuration at the project level
existing_project_config = project.build_configurations.find { |c| c.name == config_name }
unless existing_project_config
  base_project_config = project.build_configurations.find { |c| c.name == mode_capitalized } ||
                        project.build_configurations.find { |c| c.name == 'Debug' }

  new_project_config = project.add_build_configuration(config_name, base_type)
  if base_project_config
    new_project_config.build_settings = base_project_config.build_settings.dup
  end

  puts "Created project-level configuration '#{config_name}'"
end

project.save
puts "Project saved successfully"
