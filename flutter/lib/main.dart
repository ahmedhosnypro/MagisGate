import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'screens/app_list_screen.dart';

const Color trueBlack = Color(0xFF000000);
const Color electricPurple = Color(0xFF8F00FF);

void main() {
  runApp(const ProviderScope(child: RootInjectorApp()));
}

class RootInjectorApp extends StatelessWidget {
  const RootInjectorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'RootInjector',
      theme: ThemeData(
        brightness: Brightness.dark,
        primaryColor: electricPurple,
        scaffoldBackgroundColor: trueBlack,
        textTheme: Theme.of(context).textTheme.apply(bodyColor: Colors.white, displayColor: Colors.white),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white10,
          hintStyle: Theme.of(context).textTheme.bodyLarge?.copyWith(color: Colors.white54),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(30),
            borderSide: BorderSide.none,
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
        ),
        bottomSheetTheme: const BottomSheetThemeData(
          backgroundColor: Color(0xFF1A1A1A),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
        ),
      ),
      home: const AppListScreen(),
    );
  }
}
