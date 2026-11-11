cmake_minimum_required(VERSION 3.22.1)
project("openaudio_audio" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(openaudio_audio SHARED
    pinned_ring_buffer.cpp
)

# Mandatory Android 16 (API 36) 16 KB ELF Page Size Alignment
target_link_options(openaudio_audio PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

find_library(log-lib log)
target_link_libraries(openaudio_audio
    ${log-lib}
)
