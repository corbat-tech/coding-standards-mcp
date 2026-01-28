export const config = {
  jwtSecret: process.env.JWT_SECRET || 'your-secret-key',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '24h',
  port: parseInt(process.env.PORT || '3000', 10),
  bcryptRounds: 10,
};
