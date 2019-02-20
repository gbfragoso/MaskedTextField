# Changelog
All notable changes to this project will be documented in this file.

## [2.0] 2019-02-19
### Added
- License file
- Mask Factory private class
- MaskCharacter abstract class
- MaskCharacter extended classes

### Changed
- Refactoring methods
- Removing unnecessary methods
- Removing unnecessary method calls (Performance Polish)
- Change some access modifiers to private

## [1.3] 2018-04-18
### Changed
- Mask class is now a private inner class from MaskedTextField
- Some code from updadeSemanticMask() has been moved to new method "consumeText()" 

### Fixed
- IndexOutOfBound exception when text input has multiple wrong chars

## [1.2] 2017-11-08
### Added 
- Now the user can delete/replace selected text

### Fixed
- IndexOutOfBound exception at firstPlaceHolder() when mask has escape characters 

## [1.1] 2017-11-07
### Added
- Semantic Mask class

### Changed
- Now the user can use escape characters to interpret special words as literals

### Fixed
- Carret position when user insert/delete text

### Removed
- Section about "changelog" vs "CHANGELOG".

## [1.0] 2017-07-08
- Initial commit
