import { IsString, IsArray, IsOptional, MinLength, MaxLength } from 'class-validator';

/**
 * DTO for updating an existing article
 */
export class UpdateArticleDto {
  @IsString()
  @IsOptional()
  @MinLength(3, { message: 'Title must be at least 3 characters long' })
  @MaxLength(200, { message: 'Title must not exceed 200 characters' })
  title?: string;

  @IsString()
  @IsOptional()
  @MinLength(10, { message: 'Content must be at least 10 characters long' })
  content?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  tags?: string[];
}
