#include <jank/runtime/obj/character.hpp>
#include <jank/util/escape.hpp>

namespace jank::runtime
{
  static native_persistent_string get_literal_from_char(char const ch)
  {
    switch(ch)
    {
      case '\n':
        return R"(\newline)";
      case ' ':
        return R"(\space)";
      case '\t':
        return R"(\tab)";
      case '\b':
        return R"(\backspace)";
      case '\f':
        return R"(\formfeed)";
      case '\r':
        return R"(\return)";
      default:
        return fmt::format(R"(\{})", ch);
    }
  }

  obj::character::static_object(native_persistent_string const &d)
    : data{ d }
  {
  }

  obj::character::static_object(char const ch)
    : data{ 1, ch }
  {
  }

  native_bool obj::character::equal(object const &o) const
  {
    if(o.type != object_type::character)
    {
      return false;
    }

    auto const c(expect_object<obj::character>(&o));
    return data == c->data;
  }

  void obj::character::to_string(fmt::memory_buffer &buff) const
  {
    fmt::format_to(std::back_inserter(buff), "{}", data);
  }

  native_persistent_string obj::character::to_string() const
  {
    return data;
  }

  native_persistent_string obj::character::to_code_string() const
  {
    return get_literal_from_char(data[0]);
  }

  native_hash obj::character::to_hash() const
  {
    return data.to_hash();
  }
}
