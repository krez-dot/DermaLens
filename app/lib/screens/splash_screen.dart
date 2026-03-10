import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/theme.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _navigate();
  }

  Future<void> _navigate() async {
    await Future.delayed(const Duration(milliseconds: 2200));
    if (!mounted) return;
    Navigator.of(context).pushReplacementNamed('/home');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.primary,
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Logo / Icon
              Container(
                width: 110,
                height: 110,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(40),
                      blurRadius: 20,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.biotech_rounded,
                  size: 62,
                  color: AppTheme.primary,
                ),
              )
                  .animate()
                  .scale(
                    begin: const Offset(0.5, 0.5),
                    duration: 600.ms,
                    curve: Curves.elasticOut,
                  )
                  .fade(duration: 400.ms),

              const SizedBox(height: 24),

              Text(
                'DermaLens',
                style: GoogleFonts.poppins(
                  fontSize: 36,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                  letterSpacing: 1,
                ),
              )
                  .animate(delay: 300.ms)
                  .slideY(begin: 0.4, duration: 500.ms, curve: Curves.easeOut)
                  .fade(duration: 400.ms),

              const SizedBox(height: 8),

              Text(
                'AI Skin Condition Detection',
                style: GoogleFonts.poppins(
                  fontSize: 15,
                  color: Colors.white.withAlpha(204),
                  fontWeight: FontWeight.w400,
                ),
              )
                  .animate(delay: 500.ms)
                  .slideY(begin: 0.4, duration: 500.ms, curve: Curves.easeOut)
                  .fade(duration: 400.ms),

              const SizedBox(height: 60),

              const CircularProgressIndicator(
                color: Colors.white,
                strokeWidth: 2.5,
              ).animate(delay: 700.ms).fade(duration: 400.ms),
            ],
          ),
        ),
      ),
    );
  }
}
