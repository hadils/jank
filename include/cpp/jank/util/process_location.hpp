#pragma once

#if defined(__APPLE__)
  #include <mach-o/dyld.h>
#endif

#include <string>

#include <boost/filesystem.hpp>

#include <jank/option.hpp>

namespace jank::util
{
  option<boost::filesystem::path> process_location()
#if defined(__APPLE__)
  {
    uint32_t path_length{};
    if(_NSGetExecutablePath(nullptr, &path_length) != -1 || path_length <= 1)
    { return none; }

    std::string path(path_length, std::string::value_type{});
    if(_NSGetExecutablePath(path.data(), &path_length) != 0)
    { return none; }
    return boost::filesystem::canonical(path);
  }
#elif defined(__linux__)
  { return boost::filesystem::canonical("/proc/self/exe"); }
#else
  { static_assert(false, "Unsupported platform"); }
#endif
}
