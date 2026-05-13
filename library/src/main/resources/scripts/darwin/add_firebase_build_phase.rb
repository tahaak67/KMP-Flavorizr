#!/usr/bin/env ruby
# Adds a Firebase Setup build phase to the Xcode project.
# Copies the correct GoogleService-Info.plist before Copy Bundle Resources phase.
# Usage: ruby add_firebase_build_phase.rb <project_path> <target_name>

require 'xcodeproj'

project_path = ARGV[0]
target_name = ARGV[1] || 'iosApp'

project = Xcodeproj::Project.open(project_path)

app_target = project.targets.find { |t| t.name == target_name }
unless app_target
  app_target = project.targets.find { |t| t.product_type == 'com.apple.product-type.application' }
end
unless app_target
  available = project.targets.map { |t| "'#{t.name}'" }.join(', ')
  puts "ERROR: Target '#{target_name}' not found. Available targets: #{available}"
  exit 1
end

puts "Using target: '#{app_target.name}'"

copy_resources_phase = app_target.build_phases.find { |p| p.is_a?(Xcodeproj::Project::Object::PBXResourcesBuildPhase) }

if copy_resources_phase
  plist_in_resources = copy_resources_phase.files.any? do |build_file|
    build_file.file_ref&.path&.include?('GoogleService-Info.plist')
  end

  unless plist_in_resources
    target_group = project.main_group.find_subpath(target_name, false)

    if target_group && !target_group.is_a?(Xcodeproj::Project::Object::PBXFileSystemSynchronizedRootGroup)
      plist_ref = target_group.files.find { |f| f.path == 'GoogleService-Info.plist' }

      unless plist_ref
        plist_path = File.join(File.dirname(project.path), target_name, 'GoogleService-Info.plist')
        unless File.exist?(plist_path)
          puts "Creating placeholder GoogleService-Info.plist"
          File.write(plist_path, <<~PLIST)
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
            </dict>
            </plist>
          PLIST
        end
        plist_ref = target_group.new_file('GoogleService-Info.plist')
      end

      copy_resources_phase.add_file_reference(plist_ref)
      puts "Added GoogleService-Info.plist to Copy Bundle Resources phase"
    else
      puts "NOTE: Target group is synchronized (Xcode 16+), GoogleService-Info.plist will be auto-included"
    end
  else
    puts "GoogleService-Info.plist already in Copy Bundle Resources phase"
  end
else
  puts "WARNING: No Copy Bundle Resources phase found"
end

build_phase_name = 'Firebase Setup'

existing = app_target.shell_script_build_phases.find { |p| p.name == build_phase_name }
if existing
  puts "Build phase '#{build_phase_name}' already exists, updating script"
  existing.shell_script = "\"$SRCROOT/firebaseScript.sh\""
  phase = existing
else
  phase = app_target.new_shell_script_build_phase(build_phase_name)
  phase.shell_script = "\"$SRCROOT/firebaseScript.sh\""
  phase.show_env_vars_in_log = '0'
  puts "Added build phase '#{build_phase_name}'"
end

# Move Firebase Setup phase to run before Copy Bundle Resources
if copy_resources_phase
  firebase_phase_index = app_target.build_phases.index(phase)
  copy_resources_index = app_target.build_phases.index(copy_resources_phase)

  if firebase_phase_index && copy_resources_index && firebase_phase_index > copy_resources_index
    app_target.build_phases.delete_at(firebase_phase_index)
    app_target.build_phases.insert(copy_resources_index, phase)
    puts "Moved '#{build_phase_name}' phase before Copy Bundle Resources"
  end
end

project.save
puts "Project saved successfully"
