#!/usr/bin/env ruby
# Adds a file reference to an Xcode project.
# Usage: ruby add_file.rb <project_path> <file_path> <group_name> <target_name>

require 'xcodeproj'

project_path = ARGV[0]
file_path = ARGV[1]
group_name = ARGV[2]
target_name = ARGV[3] || 'iosApp'

project = Xcodeproj::Project.open(project_path)

# Find the group (try group_name first, then target_name)
group = project.main_group[group_name] || project.main_group[target_name]

# Check if we can add references to this group
can_add_reference = group &&
  group.respond_to?(:new_reference) &&
  !group.is_a?(Xcodeproj::Project::Object::PBXFileSystemSynchronizedRootGroup)

unless can_add_reference
  if group
    puts "NOTE: Group '#{group_name}' is a synchronized file group (Xcode 16+). Skipping file reference."
    puts "NOTE: Xcode will discover the file automatically from the file system."
  else
    puts "NOTE: Group '#{group_name}' not found. Skipping file reference."
  end
  project.save
  exit 0
end

# Check if file reference already exists
existing = group.files.find { |f| f.path == file_path }
if existing
  puts "File reference already exists: #{file_path}"
else
  begin
    file_ref = group.new_reference(file_path)
    puts "Added file reference: #{file_path}"
  rescue => e
    puts "NOTE: Could not add file reference: #{e.message}"
  end
end

project.save
puts "Project saved successfully"
